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
    //    传输 过去
    private SocketLive socketLive;
    //    nv21转换成nv12的数据
    byte[] nv12;
    //    旋转之后的yuv数据
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

    //    startLive  MActivity
    public void startLive() {
//        实例化编码器  MediaFormat =  hashmap  （key-value）
//        慢
        //创建对应编码器
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, height, width);
//            网络 视频通话 直播   固定码率    投屏  +事件
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //IDR帧刷新时间
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
//            周五 总的 新的知识点
            int bufferLength = width * height * 3 / 2;
            nv12 = new byte[bufferLength];
            yuv = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
//            不支持  软解
        }
    }

    ///摄像头调用
    public int encodeFrame(byte[] input) {
//        旋转
//        nv21-nv12
        nv12 = YuvUtils.nv21toNV12(input);
//        旋转
        YuvUtils.portraitData2Raw(nv12, yuv, width, height);

        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(yuv);
            long presentationTimeUs = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return 0;
    }

    //    1ms=1000us
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / 15;
    }

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
