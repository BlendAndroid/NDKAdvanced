package com.blend.ndkadvanced.screenshare.player;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityPlayerScreenShareBinding;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerScreenShareActivity extends AppCompatActivity implements PlayerSocketLive.SocketCallback {

    private static final String TAG = "PlayerScreenShare";

    private ActivityPlayerScreenShareBinding mBinding;
    private MediaCodec mediaCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPlayerScreenShareBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.sfvPlayerScreenShare.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                initSocket();

                initDecoder(holder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }


    private void initSocket() {
        PlayerSocketLive screenLive = new PlayerSocketLive(this, 12001);
        screenLive.start();
    }

    private void initDecoder(Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void callBack(byte[] data) {
        Log.i(TAG, "callBack: " + data.length);
        int index = mediaCodec.dequeueInputBuffer(100000);
//index   索引
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
//       通知dsp芯片帮忙解码
            mediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }

//        获取数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

        while (outputBufferIndex > 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}