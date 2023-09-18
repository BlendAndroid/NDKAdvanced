#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window_jni.h>
//C语言 编译器
extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include <libavutil/time.h>
#include "libswresample/swresample.h"
}

#define LOGE(...) __android_log_print(ANDROID_LOG_INFO,"BlendAndroid",__VA_ARGS__)

extern "C"
JNIEXPORT jstring JNICALL
Java_com_blend_ndkadvanced_ffmpeg_FfmpegActivity_getFfmpegConfig(JNIEnv *env, jobject thiz) {
    std::string ffmpeg_config = avcodec_configuration();
    return env->NewStringUTF(ffmpeg_config.c_str());
}

static AVFormatContext *avFormatContext; // 封装格式上下文结构体，也是统领全局的结构体，保存了视频文件封装格式相关
static AVCodecContext *avCodecContext;  // 编码器上下文结构体，保存了视频（音频）编解码相关信息
AVCodec *vCodec;    // 每种视频（音频）编解码器(例如H.264解码器)对应一个该结构体
ANativeWindow *nativeWindow;    // native绘制的窗体
ANativeWindow_Buffer windowBuffer;  // 用于保存Native Window的缓冲区信息

static AVPacket *avPacket;  // 保存解码之前的数据和一些附加信息，如显示时间戳（pts）、解码时间戳（dts）、数据时长，所在媒体流的索引等
static AVFrame *avYuvFrame, *rgbFrame; // 存放解码后的像素数据(YUV)，存放转换后的像素数据(RGB), 此外还包含一些相关信息，比如解码的时候存储宏块类型表，QP表，运动矢量等数据。
struct SwsContext *swsContext;
uint8_t *outbuffer;

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpeg_FfmpegActivity_play(JNIEnv *env, jobject thiz, jstring url_,
                                                      jobject surface) {
    const char *url = env->GetStringUTFChars(url_, 0);
    // 注册所有的组件
    avcodec_register_all();
    // 实例化了上下文, 描述了一个媒体文件或媒体流的构成和基本信息
    avFormatContext = avformat_alloc_context();

    // 打开视频文件
    if (avformat_open_input(&avFormatContext, url, NULL, NULL) != 0) {
        LOGE("Couldn't open input stream.\n");
        return;
    }
    LOGE("打开视频成功.\n");

    // 获取视频文件信息
    if (avformat_find_stream_info(avFormatContext, NULL) < 0) {
        LOGE("Couldn't find stream information.\n");
        return;
    }

    // 找到视频轨
    int videoIndex = -1;
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
        if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }
    if (videoIndex == -1) {
        LOGE("Couldn't find a video stream.\n");
        return;
    }
    LOGE("找到了视频流\n");

    // 获取编码器上下文
    avCodecContext = avFormatContext->streams[videoIndex]->codec;
    // 查找解码器
    vCodec = avcodec_find_decoder(avCodecContext->codec_id);

    // 打开解码器
    if (avcodec_open2(avCodecContext, vCodec, NULL) < 0) {
        LOGE("Couldn't open codec.\n");
        return;
    }
    LOGE("打开了解码成功\n");

    // 申请解码后的yuv和rgb数据的内存
    avYuvFrame = av_frame_alloc();
    rgbFrame = av_frame_alloc();

    // 申请解码前的数据和一些附加信息的内存
    avPacket = av_packet_alloc();

    // 根据编码器获取宽高
    int width = avCodecContext->width;
    int height = avCodecContext->height;

    // 申请用于存放解码后YUV格式数据的相关buf
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, width, height, 1);
    LOGE("计算解码后的yuv %d\n", numBytes);

    // 实例化一个yuv输入缓冲区的大小
    outbuffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));

    // 设置rgbFrame的大小,根据yuv的宽高设置
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, outbuffer,
                         AV_PIX_FMT_RGBA, width, height, 1);

    // 转换器, 将yuv格式转换为RGB格式的操作
    swsContext = sws_getContext(width, height, avCodecContext->pix_fmt,
                                width, height, AV_PIX_FMT_RGBA,
                                SWS_BICUBIC, NULL, NULL, NULL);

    // 准备native绘制的窗体, 建立了c/c++中关联surface对象ANativeWindow
    nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (0 == nativeWindow) {
        LOGE("Couldn't get native window from surface.\n");
        return;
    }

    // 设置ANativeWindow绘制窗口的属性（宽、高、像素格式）
    if (ANativeWindow_setBuffersGeometry(nativeWindow, width, height, WINDOW_FORMAT_RGBA_8888) <
        0) {
        LOGE("Couldn't set buffers geometry.\n");
    }
    LOGE("ANativeWindow_setBuffersGeometry成功\n");

    // 读取视频文件中的数据包
    while (av_read_frame(avFormatContext, avPacket) >= 0) {
        // 找出视频数据
        if (avPacket->stream_index == videoIndex) {
            // 发送数据到ffmepg，放到解码队列中
            int ret = avcodec_send_packet(avCodecContext, avPacket);
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                LOGE("解码出错");
                return;
            }

            // 将成功的解码队列中取出一帧数据, frame会每次清掉上一次frame，然后重新赋值，可以给同一个frame
            ret = avcodec_receive_frame(avCodecContext, avYuvFrame);
            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0) {
                break;
            }

            // 把yuv.AVFrame转换成rgb格式的AVFrame
            // 用于在不同像素格式之间进行图像转换和缩放
            // 参数说明：
            //- c：用于图像转换和缩放的SwsContext结构体指针。
            //- srcSlice：指向源图像数据每个平面（plane）的指针数组。
            //- srcStride：源图像数据每个平面（plane）的行大小数组。
            //- srcSliceY：源图像数据的起始行索引。
            //- srcSliceH：源图像数据的高度。
            //- dst：指向目标图像数据每个平面（plane）的指针数组。
            //- dstStride：目标图像数据每个平面（plane）的行大小数组。
            sws_scale(swsContext, avYuvFrame->data, avYuvFrame->linesize, 0, avCodecContext->height,
                      rgbFrame->data, rgbFrame->linesize);

            // 锁定窗口的下一个绘图
            if (ANativeWindow_lock(nativeWindow, &windowBuffer, nullptr) < 0) {
                LOGE("cannot lock window");
            } else {
                // 将图像绘制到界面上，注意这里pFrameRGBA一行的像素和windowBuffer一行的像素长度可能不一致
                // 将rgbFrame的数据拷贝到windowBuffer上, 需要转换好，否则可能花屏
                uint8_t *dst = (uint8_t *) windowBuffer.bits;
                // windowBuffer.stride是一行的像素长度
                for (int h = 0; h < height; h++) {
                    memcpy(dst + h * windowBuffer.stride * 4,
                           outbuffer + h * rgbFrame->linesize[0],
                           rgbFrame->linesize[0]);
                }
                switch (avYuvFrame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        LOGE("I");
                        break;
                    case AV_PICTURE_TYPE_P:
                        LOGE("P");
                        break;
                    case AV_PICTURE_TYPE_B:
                        LOGE("B");
                        break;
                    default:;
                        break;
                }
            }
            // 设置每一帧的时长
            av_usleep(1000 * 33);
            // 当yuv.AVFrame转换到rgb.AVFrame之后，rgb格式的数据直接update到所指向的内存区域
            // 然后ANativeWindow_unlockAndPost触发 底层的swap操作把新的一帧图(即windowBuffer中的数据)update到屏幕上
            ANativeWindow_unlockAndPost(nativeWindow);
        }
    }
    ANativeWindow_release(nativeWindow);
    av_frame_free(&avYuvFrame);
    av_frame_free(&rgbFrame);
    av_free(avPacket);
    sws_freeContext(swsContext);
    avcodec_close(avCodecContext);
    avcodec_free_context(&avCodecContext);
    avformat_close_input(&avFormatContext);
    avformat_free_context(avFormatContext);
    env->ReleaseStringUTFChars(url_, url);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpeg_FfmpegActivity_playSound(JNIEnv *env, jobject instance,
                                                           jstring input_) {
    const char *input = env->GetStringUTFChars(input_, 0);
    av_register_all();
//    总上下文
    AVFormatContext *pFormatCtx = avformat_alloc_context();
    if (avformat_open_input(&pFormatCtx, input, NULL, NULL) != 0) {
        LOGE("%s", "打开输入视频文件失败");
        return;
    }
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("%s", "获取视频信息失败");
        return;
    }
    int audio_stream_idx = -1;
    int i = 0;
    for (int i = 0; i < pFormatCtx->nb_streams; ++i) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            LOGE("  找到音频id %d", pFormatCtx->streams[i]->codec->codec_type);
            audio_stream_idx = i;
            break;
        }
    }
//    找到了音频索引

//找到解码器上下文
    AVCodecContext *pCodecCtx = pFormatCtx->streams[audio_stream_idx]->codec;
    //获取解码器  视频 1   音频2
    AVCodec *pCodex = avcodec_find_decoder(pCodecCtx->codec_id);
    //打开解码器
    if (avcodec_open2(pCodecCtx, pCodex, NULL) < 0) {
        return;
    }
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    //申请avframe，装解码后的数据
    AVFrame *frame = av_frame_alloc();
    int out_channer_nb = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
//转换器上下文
    SwrContext *swrContext = swr_alloc();
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_formart = AV_SAMPLE_FMT_S16;
    int out_sample_rate = pCodecCtx->sample_rate;
//    转换器的代码
    swr_alloc_set_opts(swrContext, out_ch_layout, out_formart, out_sample_rate,
//            输出的
                       pCodecCtx->channel_layout, pCodecCtx->sample_fmt, pCodecCtx->sample_rate, 0,
                       NULL
    );

//    初始化转化上下文
    swr_init(swrContext);
//    1s的pcm个数
    uint8_t *out_buffer = (uint8_t *) av_malloc(44100 * 2);
//    反射的放射的方式
    jclass david_player = env->GetObjectClass(instance);
    jmethodID createAudio = env->GetMethodID(david_player, "createTrack", "(II)V");
    env->CallVoidMethod(instance, createAudio, 44100, out_channer_nb);
    jmethodID audio_write = env->GetMethodID(david_player, "playTrack", "([BI)V");
    int got_frame;
    while (av_read_frame(pFormatCtx, packet) >= 0) {

        if (packet->stream_index == audio_stream_idx) {
//            音频的数据
            avcodec_decode_audio4(pCodecCtx, frame, &got_frame, packet);
            if (got_frame >= 0) {

//输出的我们写完了    我们再写输入数据
                swr_convert(swrContext, &out_buffer, 44100 * 2,
                            (const uint8_t **) (frame->data), frame->nb_samples);

//                解码了
                int size = av_samples_get_buffer_size(NULL, out_channer_nb, frame->nb_samples,
                                                      AV_SAMPLE_FMT_S16, 1);
//java的字节数组
                jbyteArray audio_sample_array = env->NewByteArray(size);
                env->SetByteArrayRegion(audio_sample_array, 0, size,
                                        reinterpret_cast<const jbyte *>(out_buffer));
                env->CallVoidMethod(instance, audio_write, audio_sample_array, size);
                env->DeleteLocalRef(audio_sample_array);
            }


        }


    }

    av_frame_free(&frame);
    av_free(packet);
    swr_free(&swrContext);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);
//    子线程  虚拟机绑定     jvm虚拟机

    env->ReleaseStringUTFChars(input_, input);

}