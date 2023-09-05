//
// Created by Administrator on 2021/1/25.
//

#ifndef BILIRTMP_AUDIOCHANNEL_H
#define BILIRTMP_AUDIOCHANNEL_H

#include <faac.h>
#include "../rtmp/rtmp.h"

// typedef的用法, 为复杂的声明定义一个简单别名
// 定义一个返回值是void, 参数是RTMPPacket *的函数指针类型,这个函数指针的名字叫做Callback
typedef void (*Callback)(RTMPPacket *);

class AudioChannel {
public:
    Callback callback;
    faacEncHandle codec = 0;

    // 音频压缩成aac后最大数据量
    unsigned long maxOutputBytes;
    // 输出的AAC数据
    unsigned char *outputBuffer = 0;
    // 输入容器的大小
    unsigned long inputByteNum;

public:
    AudioChannel();

    ~AudioChannel();

    void openCodec(int sampleRate, int channels);

    //编码函数
    void encode(int32_t *data, int len);

    //头帧
    RTMPPacket *getAudioConfig();

    // 传递的是一个函数指针
    void setCallback(Callback callback) {
        this->callback = callback;
    }

    int getInputByteNum() {
        return inputByteNum;
    }
};


#endif //BILIRTMP_AUDIOCHANNEL_H
