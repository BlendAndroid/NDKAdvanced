//
// Created by Administrator on 2021/1/18.
//

#include <cstring>
#include "VideoChannel.h"
#include "blendlog.h"


void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
    // 实例化X264
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;

    // 设置yuv的大小
    ySize = width * height;
    uvSize = ySize / 4;
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }

    // 声明 x264 编码器参数 : 在栈内存中声明 x264 编码器参数 , 之后对其进行赋值
    x264_param_t param;
    // 获取默认的编码器参数 : 调用 x264_param_default_preset 方法 , 可以获取 x264 编码器默认的参数
    // 设置编码速度 , 这里开发直播 , 需要尽快编码推流 , 这里设置最快的速度 ultrafast
    // 视频编码场景设置 , 这里选择 zerolatency 无延迟编码 , 同样要求最低延迟
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    // 编码规格设定, 32 对应的是 3.2 编码规格, 该规格下有指定的 码率, 帧率要求
    param.i_level_idc = 32;
    // 可设置的输入图像格式 : 经过测试 , 只有 X264_CSP_I420 格式能顺利编码成 H.264 视频数据
    param.i_csp = X264_CSP_I420;
    // 将 Camera 支持的宽高尺寸 , 设置给该 x264 编码器参数的 i_width , i_height 字段
    param.i_width = width;
    param.i_height = height;
    // 设置 B 帧个数, 这里设置没有 B 帧, 只有 I 帧和 P 帧
    // B 帧解码时, 既要参考前面的帧, 又要参考后面的帧
    // B 帧能减少传输的数据量, 但同时降低了解码速度, 直播中解码速度必须要快
    param.i_bframe = 0;
    // 码率有三种模式 : X264_RC_CQP 恒定质量 , X264_RC_CRF 恒定码率 , X264_RC_ABR 平均码率 , 这里设置一个平均码率输出
    param.rc.i_rc_method = X264_RC_ABR;
    // 设置码率, 单位是 kbps
    param.rc.i_bitrate = bitrate / 1024;
    // 帧率分子
    param.i_fps_num = fps;
    // 帧率分母
    param.i_fps_den = 1;
    //分子
    param.i_timebase_den = param.i_fps_num;
    //分母
    param.i_timebase_num = param.i_fps_den;

    // 计算帧间距的依据, 该设置表示使用 fps 帧率计算帧间距
    // 两帧之间间隔多少 fps
    // 也可以使用时间戳计算帧间距
    // 用fps而不是时间戳来计算帧间距离
    param.b_vfr_input = 0;

    // 关键帧的间距, 两个关键帧之间的距离
    // fps 表示 1 秒钟画面帧的数量, fps * 2 表示 2 秒钟的帧数
    // 该设置表示每隔 2 秒, 采集一个关键帧数据
    // 关键帧间隔时间不能太长
    // 关键帧间隔不能设置太长, 如设置 10 秒
    // 当用户1观看直播时, 不影响观看
    // 当用户2进入房间, 此时刚过去一个关键帧, 10秒内没有关键帧
    // 该用户需要等待 10 秒后收到关键帧数据后, 才有画面显示出来
    // I帧间隔
    param.i_keyint_max = fps * 2;


    // 关键帧数据 I 是否附带 SPS PPS 数据
    // 编码后, 会输出图像编码后的数据
    // 第一个图像数据输入到 x264 编码器后, 进行编码
    // 编码的第一个图像编码出来的数据 肯定是 SPS PPS 关键帧 三种数据
    // SPS PPS 作用是告知后续如何解码视频中的图像数据
    // 第二个图像数据输入到 x264 编码器后, 进行编码
    // 编码的第二个图像编码出来的数据 是 P 帧
    // 后续 n 个图像编码出 n 个 P 帧
    // 第 n + 3 个图像又编码出一个关键帧 I
    // 任何一个画面都可以编码成关键帧
    // 直播时建议设置成 1
    // 因为中途会有新用户加入, 此时该用户的播放器必须拿到 SPS PPS 才能解码画面
    // 否则无法观看视频
    // 如果设置成 0, 那么就需要开发者自己维护 SPS PPS 数据
    // 保证后来的用户可以看到直播画面
    // 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps。
    param.b_repeat_headers = 1;

    // 是否开启多线程
    param.i_threads = 1;

    // H.264有四种画质级别,分别是baseline, extended, main, high：
    //　　1、Baseline Profile：基本画质。支持I/P 帧，只支持无交错（Progressive）和CAVLC；
    //　　2、Extended profile：进阶画质。支持I/P/B/SP/SI 帧，只支持无交错（Progressive）和CAVLC；(用的少)
    //　　3、Main profile：主流画质。提供I/P/B 帧，支持无交错（Progressive）和交错（Interlaced），
    //　　　 也支持CAVLC 和CABAC 的支持；
    //　　4、High profile：高级画质。在main Profile 的基础上增加了8x8内部预测、自定义量化、 无损视频编码和更多的YUV 格式；
    //H.264 Baseline profile、Extended profile和Main profile都是针对8位样本数据、4:2:0格式(YUV)的视频序列。在相同配置情况下，
    //High profile（HP）可以比Main profile（MP）降低10%的码率。
    //根据应用领域的不同，Baseline profile多应用于实时通信领域，Main profile多应用于流媒体领域，High profile则多应用于广电和存储领域。
    // 原生的profile为“high”，直播不包含B帧，因此修改为了baseline，因为规范中只有该profile不带B帧。
    x264_param_apply_profile(&param, "baseline");

    // 打开编码器，其中初始化了libx264编码所需要的各种变量
    videoCodec = x264_encoder_open(&param);

    // 向 x264 编码图片设置
    // x264 编码图片引入 : x264 编码器对图像数据进行编码, 要先将 NV21 的图像数据中的 YUV 数据分别存储到 x264 编码图片中
    // 初始化 x264 编码图片
    pic_in = new x264_picture_t;
    // 为 x264 编码图片分配内存
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
}


VideoChannel::VideoChannel() {
}
/**
 * 将YUV编码成H264并发送
 *
 * @param data YUV数据
 */
// 这里的入参是int8_t, 也就是byte
void VideoChannel::encodeData(int8_t *data) {
    // 将 YUV 中的 Y 灰度值数据, U 色彩值数据, V 色彩饱和度数据提取出来
    memcpy(pic_in->img.plane[0], data, ySize);
    for (int i = 0; i < uvSize; ++i) {
        //提取v数据
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
        //间隔1个字节取一个数据 ,U数据
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);
    }

    // ① 普通帧 : 一般情况下, 一张图像编码出一帧数据 , pp_nal 是一帧数据, pi_nal 表示帧数为 1
    //
    // ② 关键帧 : 如果这个帧是关键帧, 那么 pp_nal 将会编码出 3 帧数据 , pi_nal 表示帧数为 3
    //
    // ③ 关键帧数据 : SPS 帧, PPS 帧, 画面帧 ;
    int pi_nal;
    //编码出的H264数据
    x264_nal_t *pp_nals;

    // 输出的图片数据
    x264_picture_t pic_out;
    // ① x264_t * 参数 : x264 视频编码器
    //
    // ② x264_nal_t **pp_nal 参数 : 编码后的帧数据, 可能是 1 帧, 也可能是 3 帧
    //
    // ③ int *pi_nal 参数 : 编码后的帧数, 1 或 3
    //
    // ④ x264_picture_t *pic_in 参数 : 输入的 NV21 格式的图片数据
    //
    // ⑤ x264_picture_t *pic_out 参数 : 输出的图片数据
    // 开始编码,将NV21编码成H264
    x264_encoder_encode(videoCodec, &pp_nals, &pi_nal, pic_in, &pic_out);

    uint8_t sps[100];
    uint8_t pps[100];


    int sps_len, pps_len;
    LOGE("编码出的帧数  %d", pi_nal);

    // 有编码出的数据
    if (pi_nal > 0) {
        for (int i = 0; i < pi_nal; ++i) {
            LOGE("输出索引:  %d  输出长度 %d", i, pi_nal);
            // 回调给Java层
//            javaCallHelper->postH264(reinterpret_cast<char *>(pp_nals[i].p_payload),pp_nals[i].i_payload);
            if (pp_nals[i].i_type == NAL_SPS) {
                // 如果是sps,减去4个字节的开始码,就是0x00000001
                sps_len = pp_nals[i].i_payload - 4;
                memcpy(sps, pp_nals[i].p_payload + 4, sps_len);
            } else if (pp_nals[i].i_type == NAL_PPS) {
                // 如果是pps,减去4个字节的开始码,就是0x00000001
                pps_len = pp_nals[i].i_payload - 4;
                memcpy(pps, pp_nals[i].p_payload + 4, pps_len);
                // 先发sps和pps
                sendSpsPps(sps, pps, sps_len, pps_len);
            } else {
                // 关键帧、非关键帧
                // ① 编码后的数据 : 编码后的 H.264 数据保存在 pp_nal[i].p_payload 中 ;
                // ② 编码后的数据长度 : 编码的 H.264 数据长度为 pp_nal[i].i_payload ;
                sendFrame(pp_nals[i].i_type, pp_nals[i].i_payload, pp_nals[i].p_payload);
            }
        }
    }
    LOGE("pi_nal  %d", pi_nal);

}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {

    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 13 + sps_len + 3 + pps_len;
    RTMPPacket_Alloc(packet, bodysize);
    int i = 0;
    // 帧类型数据 : 分为两部分;
    // 前 4 位表示帧类型, 1 表示关键帧, 2 表示普通帧
    // 后 4 位表示编码类型, 7 表示 AVC 视频编码
    // AVC sequence header 与IDR一样,都是0x17,非IDR是0x27
    packet->m_body[i++] = 0x17; // 0001 0111
    // 数据类型, 00 表示 AVC 序列头
    packet->m_body[i++] = 0x00;
    // composition time 0x000000 合成时间, 一般设置 00 00 00
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    // 版本信息
    packet->m_body[i++] = 0x01;
    // 编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    // NALU 长度
    packet->m_body[i++] = 0xFF;

    // SPS 个数
    packet->m_body[i++] = 0xE1;
    // SPS 长度, 占 2 字节
    // 设置长度的高位
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    // 设置长度的低位
    packet->m_body[i++] = sps_len & 0xff;
    // 拷贝 SPS 数据
    memcpy(&packet->m_body[i], sps, sps_len);
    // 累加 SPS 长度信息
    i += sps_len;

    //PPS 个数
    packet->m_body[i++] = 0x01;
    // PPS 数据的长度, 占 2 字节
    // 设置长度的高位
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    // 设置长度的低位
    packet->m_body[i++] = (pps_len) & 0xff;
    // 拷贝 SPS 数据
    memcpy(&packet->m_body[i], pps, pps_len);

    // 设置 RTMP 包类型, 视频类型数据
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    // 设置 RTMP 包长度
    packet->m_nBodySize = bodysize;
    // 随意分配一个管道（尽量避开rtmp.c中使用的）
    packet->m_nChannel = 10;
    // 设置视频时间戳, 如果是 SPP PPS 数据, 没有时间戳
    packet->m_nTimeStamp = 0;
    // 设置绝对时间, 对于 SPS PPS 赋值 0 即可
    packet->m_hasAbsTimestamp = 0;
    // 设置头类型, 随意设置一个
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    // 发送数据
    if (this->callback) {
        this->callback(packet);
    }
}


void VideoChannel::setVideoCallback(VideoChannel::VideoCallback callback) {
    this->callback = callback;
}

/**
 * 发送帧   关键帧 和非关键帧
 *
 * @param type 编码类型
 * @param payload   数据长度
 * @param p_payload     数据
 */
void VideoChannel::sendFrame(int type, int payload, uint8_t *p_payload) {
    //去掉开头的 00000001 / 000001
    if (p_payload[2] == 0x00) {
        payload -= 4;
        p_payload += 4; //指针向后移动4个字节
    } else if (p_payload[2] == 0x01) {
        payload -= 3;
        p_payload += 3; //指针向后移动3个字节
    }
    RTMPPacket *packet = new RTMPPacket;
    // ① 帧类型 : 1 字节, 关键帧 17, 非关键帧 27 ;
    // ② 包类型 : 1 字节, 1 表示数据帧 ( 关键帧 / 非关键帧 ), 0 表示 AVC 序列头数据 ;
    // ③ 合成时间 : 3 字节, 一般情况下设置 00 00 00 ;
    // ④ 数据长度 : 4 字节, 即真实的数据帧画面的数据大小 ;
    // 上述 帧类型 , 包类型 , 合成时间 , 数据长度 , 总共有 9 字节 , 再加上实际的 H.264
    // 数据帧长度 , 即最终打包的 RTMPPacket 数据帧大小
    int bodysize = 9 + payload;
    // 为 RTMP 数据包分配内存
    RTMPPacket_Alloc(packet, bodysize);
    // 重置 RTMP 数据包
    RTMPPacket_Reset(packet);
    // 设置帧类型, 非关键帧类型 27, 关键帧类型 17
    packet->m_body[0] = 0x27;
    //关键帧
    if (type == NAL_SLICE_IDR) {
        LOGE("关键帧");
        packet->m_body[0] = 0x17;
    }
    // 设置包类型, 01 是数据帧, 00 是 AVC 序列头封装 SPS PPS 数据
    packet->m_body[1] = 0x01;
    // 合成时间戳, AVC 数据直接赋值 00 00 00
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    // 数据长度 int 4个字节 相当于把int转成4个字节的byte数组
    packet->m_body[5] = (payload >> 24) & 0xff;
    packet->m_body[6] = (payload >> 16) & 0xff;
    packet->m_body[7] = (payload >> 8) & 0xff;
    packet->m_body[8] = (payload) & 0xff;

    // 图片数据(H.264 数据帧数据)
    memcpy(&packet->m_body[9], p_payload, payload);

    // 设置绝对时间, 对于 SPS PPS 赋值 0 即可
    packet->m_hasAbsTimestamp = 0;
    // 设置 RTMP 包长度
    packet->m_nBodySize = bodysize;
    // 设置 RTMP 包类型, 视频类型数据
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    // 分配 RTMP 通道, 随意分配
    packet->m_nChannel = 0x10;
    // 设置头类型, 随意设置一个
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}

// 只要调用该方法, x264_picture_t 必须重新进行初始化
// 因为图片大小改变了, 那么对应的图片不能再使用原来的参数了
// 释放原来的 x264_picture_t 图片, 重新进行初始化
// 析构函数中也要进行释放
// 调用 x264_picture_clean 方法释放资源 , 然后销毁对象
void VideoChannel::releaseX264Picture() {
    if (pic_in) {
        x264_picture_clean(pic_in);
        delete pic_in;
        pic_in = nullptr;
    }
}

VideoChannel::~VideoChannel() {
    if (videoCodec) {
        releaseX264Picture();
        LOGE("释放VideoChanle");
        x264_encoder_close(videoCodec);
        videoCodec = 0;
    }

}
