#include <jni.h>
#include <string>

#include <android/log.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_blend_ndkadvanced_hello_HelloWorldActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
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