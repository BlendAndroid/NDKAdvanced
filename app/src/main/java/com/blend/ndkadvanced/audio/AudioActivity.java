package com.blend.ndkadvanced.audio;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityAudioBinding;
import com.blend.ndkadvanced.utils.FileUtils;

import java.io.File;

public class AudioActivity extends AppCompatActivity {

    private ActivityAudioBinding mBinding;

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
                        // final String aacPath = new File(getExternalCacheDir(), "music.mp3").getAbsolutePath();
                        // final String outPath = getExternalCacheDir().getAbsolutePath();
                        // try {
                        //     FileUtils.copyAssets(AudioActivity.this, "music.mp3", aacPath);
                        //     new MusicMixProcess().mixAudioTrack(aacPath, outPath, 10 * 1000 * 1000, 15 * 1000 * 1000);
                        // } catch (Exception e) {
                        //     e.printStackTrace();
                        // }
                    }
                }).start();
            }
        });
    }


}