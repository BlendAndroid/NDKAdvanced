//
// Created by Administrator on 2021/1/18.
//
#ifndef BILIRTMP_VIDEOCHANNEL_H
#define BILIRTMP_VIDEOCHANNEL_H
#include <inttypes.h>
#include <jni.h>
#include <x264.h>
#include "JavaCallHelper.h"
#include "../rtmp/rtmp.h"

class VideoChannel {
    typedef void (*VideoCallback)(RTMPPacket *packet);
public:
    VideoChannel();
    ~VideoChannel();
    //创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);
//真正开始编码一帧数据
    void encodeData(int8_t *data);
    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);
//发送帧   关键帧 和非关键帧
    void sendFrame(int type, int payload, uint8_t *p_payload);
    void setVideoCallback(VideoCallback callback);
private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
//    yuv-->h264 平台 容器 x264_picture_t=bytebuffer
    x264_picture_t *pic_in = 0;
    int ySize;
    int uvSize;
//    编码器
    x264_t *videoCodec = 0;
    VideoCallback callback;

public:
    JavaCallHelper *javaCallHelper;

};
#endif
