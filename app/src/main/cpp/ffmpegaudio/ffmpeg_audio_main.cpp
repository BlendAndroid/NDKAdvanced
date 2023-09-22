#include <jni.h>
#include <string>
#include "MNFFmpeg.h"
#include "MNPlaystatus.h"

extern "C"
{
#include <libavformat/avformat.h>
}

MNCallJava *callJava = NULL;
MNFFmpeg *fFmpeg = NULL;
MNPlaystatus *playstatus = NULL;

bool nexit = true;

// 虚拟机的引用
JavaVM *javaVM = 0;

// 获取到JVM虚拟机的引用
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    LOGE("保存虚拟机的引用");
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1parpared(JNIEnv *env, jobject instance,
                                                                   jstring source_) {
    const char *source = env->GetStringUTFChars(source_, 0);

    if (fFmpeg == NULL) {
        if (callJava == NULL) {
            callJava = new MNCallJava(javaVM, env, &instance);
        }
        playstatus = new MNPlaystatus();
        fFmpeg = new MNFFmpeg(playstatus, callJava, source);
        fFmpeg->parpared();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1start(JNIEnv *env, jobject instance) {

    // TODO
    if (fFmpeg != NULL) {
        fFmpeg->start();
    }

}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1seek(JNIEnv *env, jobject thiz,
                                                               jint secds) {

    LOGE("最开始%d  ", secds);
    if (fFmpeg != NULL) {
        fFmpeg->seek(secds);
    }


}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1pause(JNIEnv *env, jobject thiz) {
    if (fFmpeg != NULL) {
        fFmpeg->pause();
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1resume(JNIEnv *env, jobject thiz) {
    // TODO: implement n_resume()
    if (fFmpeg != NULL) {
        fFmpeg->resume();
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1mute(JNIEnv *env, jobject thiz,
                                                               jint mute) {
    if (fFmpeg != NULL) {
        fFmpeg->setMute(mute);
    }


}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1volume(JNIEnv *env, jobject thiz,
                                                                 jint percent) {
    if (fFmpeg != NULL) {
        fFmpeg->setVolume(percent);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1speed(JNIEnv *env, jobject thiz,
                                                                jfloat speed) {
    if (fFmpeg != NULL) {
        fFmpeg->setSpeed(speed);
    }
}extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1pitch(JNIEnv *env, jobject thiz,
                                                                jfloat pitch) {
    if (fFmpeg != NULL) {
        fFmpeg->setPitch(pitch);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blend_ndkadvanced_ffmpegaudio_player_MNPlayer_n_1stop(JNIEnv *env, jobject thiz) {

    if (!nexit) {
        return;
    }
// 正在退出 只调用一次
    nexit = false;
    if (fFmpeg != NULL) {
        fFmpeg->release();
        delete (fFmpeg);
        if (callJava != NULL) {
            delete (callJava);
            callJava = NULL;
        }
        if (playstatus != NULL) {
            delete (playstatus);
            playstatus = NULL;
        }
    }
    nexit = true;
}