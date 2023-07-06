package com.blend.ndkadvanced.audio;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityAudioBinding;
import com.blend.ndkadvanced.utils.FileUtils;
import com.blend.ndkadvanced.utils.RangeSeekBar;

import java.io.File;
import java.io.IOException;

public class AudioActivity extends AppCompatActivity {

    private ActivityAudioBinding mBinding;

    private int musicVolume = 0;
    private int voiceVolume = 0;

    private Runnable runnable;
    private float duration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAudioBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.btnAudioClip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String aacPath = new File(getExternalCacheDir(), "music.mp3").getAbsolutePath();
                        final String outPath = getExternalCacheDir().getAbsolutePath();
                        try {
                            FileUtils.copyAssets(AudioActivity.this, "music.mp3", aacPath);
                            new MusicClipProcess().clip(aacPath, outPath, 10 * 1000 * 1000, 15 * 1000 * 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        mBinding.btnAudioMix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final String audioPath = new File(getExternalCacheDir(), "music.mp3").getAbsolutePath();
                            final String videoPath = new File(getExternalCacheDir(), "video.mp4").getAbsolutePath();
                            // FileUtils.copyAssets(AudioActivity.this, "music.mp3", audioPath);
                            // FileUtils.copyAssets(AudioActivity.this, "video.mp4", videoPath);

                            final String mixOutput = new File(getExternalCacheDir(), "mixOutput.mp3").getAbsolutePath();

                            new MusicMixProcess().mixAudioTrack(AudioActivity.this, videoPath, audioPath, mixOutput,
                                    10 * 1000 * 1000, 15 * 1000 * 1000, 100, 100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });


        mBinding.musicSeekBar.setMax(100);
        mBinding.voiceSeekBar.setMax(100);
        mBinding.musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mBinding.voiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                voiceVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        mBinding.mixAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File cacheDir = getExternalCacheDir();
                final File videoFile = new File(cacheDir, "video.mp4");
                final File audioFile = new File(cacheDir, "music.mp3");
                final File outputFile = new File(cacheDir, "output.mp4");
                if (mBinding.rangeSeekBar.getCurrentRange()[1] * 1000 * 1000 <= 0) {
                    Toast.makeText(AudioActivity.this, "重新选择", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            VideoAndMusicMixProcess.mixAudioTrack(AudioActivity.this,
                                    videoFile.getAbsolutePath(),
                                    audioFile.getAbsolutePath(),
                                    outputFile.getAbsolutePath(),
                                    (int) (mBinding.rangeSeekBar.getCurrentRange()[0] * 1000 * 1000),
                                    (int) (mBinding.rangeSeekBar.getCurrentRange()[1] * 1000 * 1000),
                                    voiceVolume,
                                    musicVolume);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AudioActivity.this, "剪辑完毕", Toast.LENGTH_SHORT).show();
                                startPlay(new File(getExternalCacheDir(), "output.mp4").getAbsolutePath());
                            }
                        });
                    }
                }.start();
            }
        });

        // TODO 这两个视频的音频不兼容
        mBinding.mixVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String videoPath1 = new File(getExternalCacheDir(), "input.mp4").getAbsolutePath();
                final String videoPath2 = new File(getExternalCacheDir(), "video.mp4").getAbsolutePath();
                final String mixVideo = new File(getExternalCacheDir(), "mixVideo.mp4").getAbsolutePath();
                new Thread() {
                    @Override
                    public void run() {

                        try {
                            // FileUtils.copyAssets(AudioActivity.this, "video.mp4", videoPath1);
                            // FileUtils.copyAssets(AudioActivity.this, "input.mp4", videoPath2);
                            VideoAddProcess.appendVideo(videoPath1, videoPath2, mixVideo);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AudioActivity.this, "合并完成", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }.start();
            }
        });
    }

    private void startPlay(String path) {
        ViewGroup.LayoutParams layoutParams = mBinding.videoView.getLayoutParams();
        layoutParams.height = 675;
        layoutParams.width = 1285;
        mBinding.videoView.setLayoutParams(layoutParams);
        mBinding.videoView.setVideoPath(path);

        mBinding.videoView.start();
        mBinding.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mp.getDuration() <= 0) {
                    Toast.makeText(AudioActivity.this, "格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                duration = mp.getDuration() / 1000;
                if (duration <= 0) {
                    Toast.makeText(AudioActivity.this, "duration 格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                mp.setLooping(true);
                mBinding.rangeSeekBar.setRange(0, duration);
                mBinding.rangeSeekBar.setValue(0, duration);
                mBinding.rangeSeekBar.setEnabled(true);
                mBinding.rangeSeekBar.requestLayout();
                mBinding.rangeSeekBar.setOnRangeChangedListener(new RangeSeekBar.OnRangeChangedListener() {
                    @Override
                    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
                        mBinding.videoView.seekTo((int) min * 1000);
                    }
                });
                final Handler handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (mBinding.videoView.getCurrentPosition() >= mBinding.rangeSeekBar.getCurrentRange()[1] * 1000) {
                            mBinding.videoView.seekTo((int) mBinding.rangeSeekBar.getCurrentRange()[0] * 1000);
                        }
                        handler.postDelayed(runnable, 1000);
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        });
    }


}