//
// Created by Administrator on 2021/1/22.
//

#include "JavaCallHelper.h"
#include "blendlog.h"

/**
 *
 * @param _javaVM   Java虚拟机
 * @param _env    JNI环境
 * @param _jobj     LivePush对象
 */
JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) : javaVM(_javaVM),
                                                                                env(_env) {
    // 局部变量变成全局变量
    jobj = env->NewGlobalRef(_jobj);
    // JNI 中调用 Java 方法流程
    // 1.获取 jclass 类型变量
    jclass jclazz = env->GetObjectClass(jobj);
    // 2.通过反射获取方法(签名规则 : (参数1类型签名参数2类型签名参数3类型签名参数N类型签名...)返回值类型签名, 注意参数列表中没有任何间隔)
    jmid_postData = env->GetMethodID(jclazz, "postData", "([B)V");  // private void postData(byte[] data)
}

void JavaCallHelper::postH264(char *data, int length, int thread) {
    // 将c的byte数组转换成java的byte数组
    jbyteArray array = env->NewByteArray(length);
    // 将c的byte数组拷贝到java的byte数组中
    env->SetByteArrayRegion(array, 0, length, reinterpret_cast<const jbyte *>(data));

    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        //  调用 JavaVM 的 AttachCurrentThread 方法 , 可以绑定线程 , 其传入一个 JNIEnv ** 二维指针 ,
        //  会返回该线程对应的 JNIEnv 指针 ;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }

        jniEnv->CallVoidMethod(jobj, jmid_postData, array);
        //  注意使用完 JNIEnv 后 , 解绑线程 , 调用 JavaVM 的 DetachCurrentThread 方法 解绑线程 ;
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_postData, array);
    }


}

JavaCallHelper::~JavaCallHelper() {
    LOGE("JavaCallHelper");
    env->DeleteGlobalRef(jobj);
    jobj = 0;
}
