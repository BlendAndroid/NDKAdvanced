package com.blend.ndkadvanced.rtmp;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.view.Surface;

import com.blend.ndkadvanced.utils.DefaultPoolExecutor;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoCodec implements Runnable {
    //    录屏工具类
    private MediaProjection mediaProjection;
    //    虚拟的画布
    private VirtualDisplay virtualDisplay;

    private MediaCodec mediaCodec;
    //传输层的引用
    private ScreenLiveService screenLive;

    public VideoCodec(ScreenLiveService screenLive) {
        this.screenLive = screenLive;
    }

    //    每一帧编码时间
    private long timeStamp;
    //    开始时间
    private long startTime;
    //    编码
    private boolean isLiving;

    @Override
    public void run() {
//        投屏  ----Meadiaprotion
        isLiving = true;
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        手动触发I帧
        while (isLiving) {
            if (System.currentTimeMillis() - timeStamp >= 2000) {
                Bundle params = new Bundle();
//短信
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
//                dsp 芯片触发I帧
                mediaCodec.setParameters(params);
                timeStamp = System.currentTimeMillis();
            }
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >= 0) {
                if (startTime == 0) {
//                    毫秒
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
//                I帧       2s
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                MediaFormat mediaFormat = mediaCodec.getOutputFormat(index);
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
//封账javabean
                RTMPPackage rtmpPackage = new RTMPPackage(outData, (bufferInfo.presentationTimeUs / 1000) - startTime);
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_VIDEO);
                screenLive.addPackage(rtmpPackage);
                mediaCodec.releaseOutputBuffer(index, false);
            }
        }
        isLiving = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        virtualDisplay.release();
        virtualDisplay = null;
        mediaProjection.stop();
        mediaProjection = null;
        startTime = 0;

    }

    //    开启编码层   初始化编码层
    public void startLive(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                720,
                1280);

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
//        帧率比较低     直播中I  250  400    视频  极限压缩  短视频 帧率   33帧      60帧  完美压缩了
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
//重新来一遍吗

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");//手机
            mediaCodec.configure(format, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "screen-codec",
                    720, 1280, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface, null, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
        DefaultPoolExecutor.getInstance().execute(this);
    }

    public void stopLive() {
        isLiving = false;
    }
}
