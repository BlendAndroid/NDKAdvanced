//
// Created by Administrator on 2021/1/25.
//

#include <malloc.h>
#include <cstring>
#include "AudioChannel.h"
#include "blendlog.h"

AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {

}

/**
 * 实例化faac编码器及参数
 *
 * @param sampleRate    采样率
 * @param channels    声道数
 */
void AudioChannel::openCodec(int sampleRate, int channels) {

    // maxOutputBytes 编码, 输出数据最大值

    // 输入数据的大小
    unsigned long inputSamples;

    // 实例化faac编码器
    /**
     unsigned long   nSampleRate,        // 采样率，单位是bps
     unsigned long   nChannels,          // 声道，1为单声道，2为双声道
     unsigned long   &nInputSamples,     // 传引用，得到每次调用编码时所应接收的原始数据长度
     unsigned long   &nMaxOutputBytes    // 传引用，得到每次调用编码时生成的AAC数据的最大长度
     */
    codec = faacEncOpen(sampleRate, channels, &inputSamples, &maxOutputBytes);

    // 输入容器真正大小
    inputByteNum = inputSamples * 2;    // 这里的2是根据: pcm位深 / 8 = 2算出来的，pcm位深用于计算一帧pcm大小

    //实例化 输出的容器
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));
    LOGE("初始化-----------》%d  inputByteNum %d  maxOutputBytes:%d ", codec, inputByteNum,
         maxOutputBytes);

    // 参数
    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(codec);
    // 编码  MPEG AAC
    configurationPtr->mpegVersion = MPEG4;
    // 编码等级
    configurationPtr->aacObjectType = LOW;
    // 输出aac裸流数据
    configurationPtr->outputFormat = 0;
    // 采样位数
    configurationPtr->inputFormat = FAAC_INPUT_16BIT;
    // 将我们的配置生效
    faacEncSetConfiguration(codec, configurationPtr);
}

void AudioChannel::encode(int32_t *data, int len) {

    LOGE("发送音频%d", len);

    // 开始编码，codec为编码器句柄，data为PCM数据，len为打开编码器时得到的输入样本数据的大小
    // outputBuffer为编码后数据存放位置，maxOutputBytes为编码后最大输出字节数，bytelen为编码后数据长度
    // 将pcm数据编码成aac数据
    int bytelen = faacEncEncode(codec, data, len, outputBuffer, maxOutputBytes);

    // ret为0时不代表编码失败，而是编码速度较慢，导致缓存还未完全flush，可用一个循环继续调用编码接口，当 ret>0 时表示编码成功，且返回值为编码后数据长度
    while (bytelen == 0) {
        bytelen = faacEncEncode(codec, data, len, outputBuffer, maxOutputBytes);
    }

    // 编码成功
    if (bytelen > 0) {
        // 拼装音频数据packet
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, bytelen + 2);
        packet->m_body[0] = 0xAF;
        packet->m_body[1] = 0x01;   // 0x01表示音频数据
        memcpy(&packet->m_body[2], outputBuffer, bytelen);
        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bytelen + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        // 回调给x64_faac_main.cpp中的callBack函数
        callback(packet);
    }
}

// 设置音频的头帧
RTMPPacket *AudioChannel::getAudioConfig() {
    u_char *buf;
    u_long len;
    //头帧的内容  先发 {0x12 0x08},也就是Audio Specific config
    faacEncGetDecoderSpecificInfo(codec, &buf, &len);
    //头帧的  rtmpdump  实时录制  实时给时间戳
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, len + 2);

    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x00;
    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = len + 2;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}