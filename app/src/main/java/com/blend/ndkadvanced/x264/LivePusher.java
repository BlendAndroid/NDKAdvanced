package com.blend.ndkadvanced.x264;

import android.util.Log;

public class LivePusher {

    public LivePusher() {
        native_init();
    }

    public void startLive(String path) {
        native_start(path);
    }

    //    jni回调java层的方法  byte[] data    char *data
    private void postData(byte[] data) {

        Log.i("rtmp", "postData: " + data.length);
        // FileUtils.writeBytes(data);
        // FileUtils.writeContent(data);
    }

    public native void native_init();

    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);

    public native void native_start(String path);

    public native void native_pushVideo(byte[] data);

    public native void native_release();

    public native int init_audioEnc(int sampleRate, int channels);

    public native void native_sendAudio(byte[] buffer, int len);

}
