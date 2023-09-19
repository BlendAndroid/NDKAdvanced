package com.blend.ndkadvanced.ffmpeg;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityFfmpegBinding;

import java.io.File;

public class FfmpegActivity extends AppCompatActivity {

    private ActivityFfmpegBinding binding;
    private Surface mSurface;

    private AudioTrack audioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFfmpegBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnFfmpegSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        binding.btnFfmpegTest.setOnClickListener(v -> {
            String ffmpegConfig = getFfmpegConfig();
            Toast.makeText(FfmpegActivity.this, ffmpegConfig, Toast.LENGTH_SHORT).show();
        });

        binding.btnFfmpegVideo.setOnClickListener(v -> {
            playVideo();
        });

        binding.btnFfmpegAudio.setOnClickListener(v -> {
            File file = new File(getExternalCacheDir(), "input.mp4");
            playSound(file.getAbsolutePath());
        });
    }

    public void playVideo() {
        String path = new File(getExternalCacheDir(), "input.mp4").getAbsolutePath();
        play(path, mSurface);
    }

    public native String getFfmpegConfig();

    public native void play(String url, Surface surface);

    public native void playSound(String input);

    // AudioTrack 有两种数据加载模式（MODE_STREAM 和 MODE_STATIC）， 对应着两种完全不同的使用场景。
    // MODE_STREAM：在这种模式下，通过 write 一次次把音频数据写到 AudioTrack 中。这和平时通过 write 调用往文件中写数据类似，
    // 但这种方式每次都需要把数据从用户提供的 Buffer 中拷贝到 AudioTrack 内部的 Buffer 中，在一定程度上会使引起延时。为解决这
    // 一问题，AudioTrack 就引入了第二种模式。
    // MODE_STATIC：在这种模式下，只需要在 play 之前通过一次 write 调用，把所有数据传递到 AudioTrack 中的内部缓冲区，后续就
    // 不必再传递数据了。这种模式适用于像铃声这种内存占用较小、延时要求较高的文件。但它也有一个缺点，就是一次 write 的数据不能太多，
    // 否则系统无法分配足够的内存来存储全部数据。

    // native层回调，使用 AudioTrack 播放 PCM 数据流
    // 采样率:44100  通道数:2
    public void createTrack(int sampleRateInHz, int channels) {
        Toast.makeText(this, "初始化播放器", Toast.LENGTH_SHORT).show();
        int channelConfig; //通道数

        if (channels == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (channels == 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        }
        int bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    public void playTrack(byte[] buffer, int length) {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(buffer, 0, length);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}