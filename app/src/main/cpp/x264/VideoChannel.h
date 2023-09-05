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
    // // 定义一个返回值是void, 参数是RTMPPacket *的函数指针类型,这个函数指针的名字叫做VideoCallback
    typedef void (*VideoCallback)(RTMPPacket *packet);
public:
    VideoChannel();
    ~VideoChannel();
    // 创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);
    // 真正开始编码一帧数据
    void encodeData(int8_t *data);
    // 发送sps和pps
    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);
    // 发送帧   关键帧 和非关键帧
    void sendFrame(int type, int payload, uint8_t *p_payload);
    // 设置RTMP包回调
    void setVideoCallback(VideoCallback callback);
    // 释放x264_picture_t
    void releaseX264Picture();

    // Java层回调
    JavaCallHelper *javaCallHelper;
private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;

    // TODO 还需要释放
    // x264 需要编码的图片
    x264_picture_t *pic_in = 0;
    // Y数据的大小
    int ySize;
    // UV数据的大小
    int uvSize;
    // 编码器
    x264_t *videoCodec = 0;
    VideoCallback callback;

};
#endif
