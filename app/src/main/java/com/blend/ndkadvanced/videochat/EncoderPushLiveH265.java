package com.blend.ndkadvanced.videochat;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.blend.ndkadvanced.socket.SocketLive;
import com.blend.ndkadvanced.utils.YuvUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class EncoderPushLiveH265 {
    private static final String TAG = "EncoderPushLiveH265";
    private MediaCodec mediaCodec;
    int width;
    int height;
    private final SocketLive socketLive;
    // nv21转换成nv12的数据
    byte[] nv12;
    // 旋转之后的yuv数据
    byte[] yuv;
    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    private byte[] vps_sps_pps_buf;
    int frameIndex;

    public EncoderPushLiveH265(SocketLive socketLive, int width, int height) {
        this.socketLive = socketLive;
//        建立连接
        this.socketLive.start();
        this.width = width;
        this.height = height;
    }


    public void startLive() {
        // 创建对应编码器
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, height, width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            // COLOR_FormatYUV420Flexible是一种灵活的YUV 420格式
            // 具体来说，COLOR_FormatYUV420Flexible可以接受以下三种YUV 420数据存储格式的输入：
            // 1. Planar YUV 420 (YUV420p)(I420)
            // 2. Semi-Planar YUV 420 with interleaved UV planes (NV12)
            // 3. Semi-Planar YUV 420 with UV planes in separate blocks (NV21)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //IDR帧刷新时间
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            int bufferLength = width * height * 3 / 2;
            nv12 = new byte[bufferLength];
            yuv = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
//            不支持  软解
        }
    }

    //摄像头数据,开始编码数据
    public int encodeFrame(byte[] input) {
//        nv21转换为nv12
        nv12 = YuvUtils.nv21toNV12(input);
//        将旋转后的数据保存到yuv中
        YuvUtils.portraitData2Raw(nv12, yuv, width, height);

        // 输入到dsp芯片
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(yuv);
            long presentationTimeUs = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }

        // 拿到dsp芯片的输出
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            // 不断的从输出缓冲区拿数据
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return 0;
    }

    // 1秒（s）= 1000毫秒（ms）
    // 1毫秒（ms）= 1000微秒（μs）
    // 1微秒（μs）= 1000纳秒（ns）
    // 这里的是以微妙为单位, microseconds（μs）
    private long computePresentationTime(long frameIndex) {
        // 偏移132,因为是从第0帧开始的,要偏移,要是刚开始会黑屏一下
        return 132 + frameIndex * 1_000_000 / 15;   // 15帧每秒
    }

    // 将每个I帧前面加上vps, sps, pps
    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = (bb.get(offset) & 0x7E) >> 1;
        if (type == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            bb.get(vps_sps_pps_buf);
        } else if (type == NAL_I) {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            byte[] newBuf = new byte[vps_sps_pps_buf.length + bytes.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);
            this.socketLive.sendData(newBuf);
        } else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.socketLive.sendData(bytes);
            Log.e(TAG, "发送视频数据：  " + Arrays.toString(bytes));
        }
    }
}
