package com.blend.ndkadvanced.rtmp;

public class RTMPPackage {

    // 视频类型
    public static final int RTMP_PACKET_TYPE_VIDEO = 0;
    // 音频头
    public static final int RTMP_PACKET_TYPE_AUDIO_HEAD = 1;
    // 音频信息
    public static final int RTMP_PACKET_TYPE_AUDIO_DATA = 2;

    //    帧数据
    private byte[] buffer;
    //    时间戳
    private long tms;

    // 数据类型
    private int type;

    public RTMPPackage( ) {
    }

    public RTMPPackage(byte[] buffer, long tms) {
        this.buffer = buffer;
        this.tms = tms;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public long getTms() {
        return tms;
    }

    public void setTms(long tms) {
        this.tms = tms;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
