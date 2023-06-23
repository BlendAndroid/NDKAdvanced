package com.blend.ndkadvanced.golomb;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityGolombBinding;

public class GolombActivity extends AppCompatActivity {

    private ActivityGolombBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityGolombBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.btnH264Golomb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GolombDemo.Ue();
            }
        });

        mBinding.btnH264Codec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaCodec mediaCodec = new MediaCodec(getBaseContext().getExternalCacheDir() + "/codec.h264");
                mediaCodec.startCodec();
            }
        });

    }


}