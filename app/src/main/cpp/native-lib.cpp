#include <jni.h>
#include <string>

// 以C的方式加载
extern "C" {
#include "gif_lib.h"
}

#include <android/log.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_blend_ndkadvanced_hello_HelloWorldActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

/**********************************Gif播放*************************************************/

// 一共8个字节
struct GifBean {
    int current_frame;  // 播放的当前帧
    int total_frame;    //总帧数
    int *delays;    //每一帧的延迟
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_loadGif(JNIEnv *env, jclass clazz, jstring path_) {
    // java的String转化为C的常量字符串，是一个字符数组，需要释放
    const char *path = env->GetStringUTFChars(path_, 0);

    int ERROR;

    // 打开gif文件，传递Error的地址
    GifFileType *gifFileType = DGifOpenFileName(path, &ERROR);

    if (gifFileType == nullptr) {
        return ERROR;
    }

    // 初始化缓冲区
    DGifSlurp(gifFileType);

    // 分配内存
    GifBean *gifBean = static_cast<struct GifBean *>(malloc(sizeof(struct GifBean)));
    // 移除脏数据
    memset(gifBean, 0, sizeof(GifBean));
    // 设置TAG，UserData是一个void类型，可以赋值给任何类型
    gifFileType->UserData = gifBean;
    // 当前帧是第一帧，也就是0
    gifBean->current_frame = 0;
    // 总帧数
    gifBean->total_frame = gifFileType->ImageCount;

    // 释放这个String，第一个参数是java的String
    env->ReleaseStringUTFChars(path_, path);

    // 返回一个指针，是long类型的，因为指针占用8个字节，long也是8个字节
    return (jlong) (gifFileType);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_getWidth(JNIEnv *env, jclass clazz, jlong gif_handler) {
    // 强转成指针类型
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_handler);
    // 返回宽度
    return gifFileType->SWidth;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_getHeight(JNIEnv *env, jclass clazz, jlong gif_handler) {
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_handler);
    return gifFileType->SHeight;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_updateFrame(JNIEnv *env, jclass clazz, jlong gif_point,
                                                      jobject bitmap) {
    // TODO: implement updateFrame()
}