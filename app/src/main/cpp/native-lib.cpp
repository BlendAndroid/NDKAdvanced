#include <jni.h>
#include <string>

#include <android/bitmap.h>

// 以C的方式加载
extern "C" {
#include "gif/gif_lib.h"
}

#include <android/log.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_blend_ndkadvanced_hello_HelloWorldActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

// 全局的string
jclass stringClass;

// static 定义的就是jclass的变量
extern "C"
JNIEXPORT jstring JNICALL
Java_com_blend_ndkadvanced_hello_HelloWorldActivity_staticString(JNIEnv *env, jclass clazz) {
    jclass tempClass = env->FindClass("java/lang/String");
    stringClass = static_cast<jclass>(env->NewGlobalRef(tempClass));

    //合适的地方销毁
    env->DeleteGlobalRef(stringClass);
    if (stringClass == nullptr) {
        return nullptr;
    }
    return (jstring) "-1";
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

    __android_log_print(ANDROID_LOG_INFO, "loadGif path", "%s", path);

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

}

/**********************************动态注册*************************************************/

jstring blend(
        JNIEnv *env,
        jobject job) {
    std::string hello = "Hello from C++ Dynamic";
    return env->NewStringUTF(hello.c_str());
}

// 利用结构体 JNINativeMethod 数组记录 java 方法与 JNI 函数的对应关系
static const JNINativeMethod gMethods[] = {
        {
                "stringFromJNIDynamic", //Java方法名
                "()Ljava/lang/String;", //方法签名（参数和返回值）
                (jstring *) blend   //函数指针
        }
};

// 实现动态注册，利用 RegisterNatives 方法来注册 java 方法与 JNI 函数的一一对应关系；
// 实现 JNI_OnLoad 方法，在加载动态库后，执行动态注册
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    // 答应log_info的日志
    __android_log_print(ANDROID_LOG_INFO, "native", "Jni_OnLoad");
    // 定义一个指针，获取env的值，第二个参数是JNI的版本
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    // 调用 FindClass 方法，获取 java 对象
    jclass clazz = env->FindClass("com/blend/ndkadvanced/hello/HelloWorldActivity");

    // 调用 RegisterNatives 方法，传入 java 对象，以及 JNINativeMethod 数组，以及注册数目完成注册
    env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0]));
    return JNI_VERSION_1_4;
}