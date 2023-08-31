package com.blend.ndkadvanced.videochat;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecoderPlayerLiveH265 {
    private static final String TAG = "DecoderPlayerLiveH265";
    private MediaCodec mediaCodec;

    //TODO 这里的宽高应该是自适应的，并且这里的实现是Camera实现的，在高版本的手机上会出现问题，会花屏
    public void initDecoder(Surface surface, int width, int height) {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/hevc");
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void callBack(byte[] data) {
        // 获取输入缓冲区的索引
        int index = mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            //dsp芯片解码, 只需要保证编码顺序就好了
            mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
        }

        // 获取到解码后的数据编码
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}
