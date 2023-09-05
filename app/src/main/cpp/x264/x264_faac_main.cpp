#include <jni.h>
#include <string>
#include <android/log.h>
#include <malloc.h>
#include "VideoChannel.h"
#include "blendlog.h"
#include "safe_queue.h"
#include "AudioChannel.h"

extern "C" {
#include "../rtmp/rtmp.h"
}

VideoChannel *videoChannel = nullptr;
AudioChannel *audioChannel = nullptr;
JavaCallHelper *helper = nullptr;
int isStart = 0;

// 记录子线程的对象, 开始推流工作线程的线程 ID
pthread_t pid;
// 推流标志位
int readyPushing = 0;
// 阻塞式队列
SafeQueue<RTMPPacket *> packets;

uint32_t start_time;
// RTMP对象
RTMP *rtmp = 0;
// 虚拟机的引用
JavaVM *javaVM = 0;

// typedef void (*Callback)(RTMPPacket *); 函数指针类型的实现
void callBack(RTMPPacket *packet) {
    if (packet) {
        if (packets.size() > 50) {
            packets.clear();
        }
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

// 获取到JVM虚拟机的引用
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    LOGE("保存虚拟机的引用");
    return JNI_VERSION_1_4;
}

void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}

void *start(void *args) {
    char *url = static_cast<char *>(args);
    do {
        // 创建 RTMP 对象, 申请内存
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp创建失败");
            break;
        }
        // 初始化 RTMP
        RTMP_Init(rtmp);
        // 设置超时时间 5s
        rtmp->Link.timeout = 5;
        // 设置 RTMP 推流地址
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        // 启用 RTMP 写出功能
        RTMP_EnableWrite(rtmp);
        // 连接 RTMP 服务器
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        // 连接 RTMP 流
        ret = RTMP_ConnectStream(rtmp, 0);

        LOGE("rtmp连接成功----------->:%s", url);
        if (!ret) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }

        // 准备好了 可以开始推流了
        readyPushing = 1;
        // 记录第一个开始推流的时间
        start_time = RTMP_GetTime();

        // 线程安全队列开始工作
        packets.setWork(1);

        // 获取到音频的头帧,开始发送
        RTMPPacket *audioHeader = audioChannel->getAudioConfig();
        callBack(audioHeader);

        RTMPPacket *packet = 0;
        //循环从队列取包 然后发送
        while (isStart) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }
            if (!packet) {
                continue;
            }
            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            //发送包 1:加入队列发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("发送数据失败");
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_native_1init(JNIEnv *env, jobject thiz) {

    // 设置Java回调回调
    helper = new JavaCallHelper(javaVM, env, thiz);

    // 实例化视频编码层
    videoChannel = new VideoChannel;
    // 设置回调
    videoChannel->setVideoCallback(callBack);
    videoChannel->javaCallHelper = helper;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                   jint width, jint height,
                                                                   jint fps, jint bitrate) {
    // 如果videoChannel不为空,初始化x264编码器
    if (videoChannel) {
        videoChannel->setVideoEncInfo(width, height, fps, bitrate);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring path_) {
    // 链接rtmp服务器,在子线程中
    if (isStart) {
        return;
    }
    const char *path = env->GetStringUTFChars(path_, 0);

    // 末尾加上'\0'
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);
    // 开始直播
    isStart = 1;

    //参数说明：
    //- `thread`：指向pthread_t类型的指针，用于存储新创建的线程的标识符。
    //- `attr`：指向pthread_attr_t类型的指针，用于指定新线程的属性。可以传入NULL使用默认属性。
    //- `start_routine`：指向函数指针，该函数是线程的入口点，在新线程中运行。它具有一个`void*`类型的参数和返回值。
    //- `arg`：传递给`start_routine`函数的参数。
    //
    //返回值：
    //- 成功创建线程时，返回0。
    //- 创建线程失败时，返回一个非零的错误码。
    // 开子线程,用于链接B站服务器,调用start方法
    pthread_create(&pid, 0, start, url);

    env->ReleaseStringUTFChars(path_, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz,
                                                             jbyteArray data_) {

    if (!videoChannel || !readyPushing) {
        return;
    }
    // 将java的byte数组转换成c的byte数组
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    // 将YUV编码成H264
    videoChannel->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_native_1release(JNIEnv *env, jobject thiz) {
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = nullptr;
    }
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = nullptr;
    }
    if (audioChannel) {
        delete (audioChannel);
        audioChannel = nullptr;
    }
    if (helper) {
        delete (helper);
        helper = nullptr;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_init_1audioEnc(JNIEnv *env, jobject thiz,
                                                          jint sample_rate, jint channels) {
    // 初始化音频faac编码
    audioChannel = new AudioChannel();
    // 设置回调函数
    audioChannel->setCallback(callBack);
    audioChannel->openCodec(sample_rate, channels);
    return audioChannel->getInputByteNum();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_x264_LivePusher_native_1sendAudio(JNIEnv *env, jobject thiz,
                                                             jbyteArray buffer, jint len) {
    if (!audioChannel || !readyPushing) {
        return;
    }
    // 转换为C层的字节数组
    jbyte *data = env->GetByteArrayElements(buffer, 0);
    // 编码
    audioChannel->encode(reinterpret_cast<int32_t *>(data), len);
    // 释放掉
    env->ReleaseByteArrayElements(buffer, data, 0);
}