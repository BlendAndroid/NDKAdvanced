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
import com.blend.ndkadvanced.socket.PlayerSocketLive;
import com.blend.ndkadvanced.socket.SocketCallback;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayerScreenShareActivity extends AppCompatActivity implements SocketCallback {

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
        PlayerSocketLive screenLive = new PlayerSocketLive(this);
        screenLive.start();
    }

    private void initDecoder(Surface surface) {
        try {
            // h265编码
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            // 将数据放在surface中进行展示
            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void callBack(byte[] data) {
        Log.i(TAG, "callBack: " + data.length);
        /*
        当使用MediaCodec API进行视频编码时，需要将待编码的视频数据放入编码器的输入缓冲区中。如果编码器使用的
        是Surface作为输入，则不需要放入数据到缓冲区，而是将数据直接发送到Surface上。如果编码器使用的是ByteBuffer
        作为输入，则需要向编码器的输入缓冲区中放入待编码的视频数据。
         */
        // 获取输入缓冲区的索引
        int index = mediaCodec.dequeueInputBuffer(100000);
        // index   索引
        if (index >= 0) {
            // 获取到输入缓冲区的ByteBuffer
            /*
            在使用ByteBuffer作为输入时，可以使用`getInputBuffer(int index)`方法获取编码器的输入缓冲区。
            其中，index表示要获取的输入缓冲区的索引。获取到的输入缓冲区是一个ByteBuffer对象，可以向其中写入
            待编码的视频数据。
             */
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            // 将数据放入输入缓冲区
            inputBuffer.put(data, 0, data.length);
            // 将数据放入输入缓冲区,开始通知dsp芯片帮忙解码
            mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
        }
        Log.i(TAG, "index: " + index);

        // 获取解码后的数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        // 获取输出缓冲区的索引
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100_000);
        Log.i(TAG, "outputBufferIndex: " + outputBufferIndex);

        // 这里是while语句，如果缓存数据没有解析完成，就一直解析
        /*
        当使用MediaCodec API进行视频编解码时，编码器会将编码后的视频数据放入其输出缓冲区中，这些数据可以被用于保存
        到文件、通过网络发送等操作。在使用编码器的输出数据时，需要先使用`dequeueOutputBuffer()`方法获取一个可用的
        输出缓冲区，并处理其中的数据。在处理完数据后，需要使用`releaseOutputBuffer()`方法将输出缓冲区释放。
         */
        while (outputBufferIndex > 0) {
            // 开始解码,并且在surface中显示
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}