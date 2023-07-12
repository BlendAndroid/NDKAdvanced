package com.blend.ndkadvanced.camerax;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityCameraXBinding;

public class CameraXActivity extends AppCompatActivity {

    private ActivityCameraXBinding mBinding;
    private CameraXHelper mCameraXHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCameraXBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mCameraXHelper = new CameraXHelper(this, mBinding.cameraXTextureView);

        mBinding.btnCameraXCapture.setOnClickListener(
                v -> mCameraXHelper.captureImage(mBinding.imgCameraXBitmap)
        );

    }
}