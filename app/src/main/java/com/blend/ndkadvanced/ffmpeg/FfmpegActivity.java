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

    // native层回调  调用 sampleRateInHz  采样评率  44100  通道数   2
    public void createTrack(int sampleRateInHz, int nb_channals) {
        Toast.makeText(this, "初始化播放器", Toast.LENGTH_SHORT).show();
        int channaleConfig;//通道数

        if (nb_channals == 1) {
            channaleConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (nb_channals == 2) {
            channaleConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            channaleConfig = AudioFormat.CHANNEL_OUT_MONO;
        }
        int buffersize = AudioTrack.getMinBufferSize(sampleRateInHz, channaleConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channaleConfig, AudioFormat.ENCODING_PCM_16BIT, buffersize, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    public void playTrack(byte[] buffer, int lenth) {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(buffer, 0, lenth);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}