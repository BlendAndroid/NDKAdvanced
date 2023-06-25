package com.blend.ndkadvanced.camera1;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityCamera1Binding;

public class Camera1Activity extends AppCompatActivity {

    private ActivityCamera1Binding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCamera1Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBinding.btnCamera1Capture.setOnClickListener(v -> {
                    mBinding.SurfaceCamera1.startCapture(mBinding.imgCamera1Bitmap);
                }
        );

    }
}