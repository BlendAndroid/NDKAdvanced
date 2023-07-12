//
// Created by Administrator on 2021/1/22.
//

#include "JavaCallHelper.h"
#include "blendlog.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) : javaVM(_javaVM),
                                                                                env(_env) {
//    局部变量变成 全局变量  jni
    jobj = env->NewGlobalRef(_jobj);
    jclass jclazz = env->GetObjectClass(jobj);
    jmid_postData = env->GetMethodID(jclazz, "postData", "([B)V");
}

void JavaCallHelper::postH264(char *data, int length, int thread) {
//byte[]
    jbyteArray array = env->NewByteArray(length);
    env->SetByteArrayRegion(array, 0, length, reinterpret_cast<const jbyte *>(data));

    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
            return;
        }

        jniEnv->CallVoidMethod(jobj, jmid_postData,array);
        javaVM->DetachCurrentThread();
    } else {
        env->CallVoidMethod(jobj, jmid_postData,array);
    }


}
JavaCallHelper::~JavaCallHelper() {
    LOGE("JavaCallHelper");
    env->DeleteGlobalRef(jobj);
    jobj = 0;
}
