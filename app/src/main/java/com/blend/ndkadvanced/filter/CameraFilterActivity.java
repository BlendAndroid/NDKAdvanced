package com.blend.ndkadvanced.filter;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityCameraFilterBinding;

public class CameraFilterActivity extends AppCompatActivity {

    private ActivityCameraFilterBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCameraFilterBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.btnFilter.setOnClickListener(v -> {
                    mBinding.btnFilterCameraView.setFragShader(0);
                }
        );
        mBinding.btnFilter1.setOnClickListener(v -> {
                    mBinding.btnFilterCameraView.setFragShader(1);
                }
        );
        mBinding.btnFilter2.setOnClickListener(v -> {
                    mBinding.btnFilterCameraView.setFragShader(2);
                }
        );
        mBinding.btnFilter3.setOnClickListener(v -> {
                    mBinding.btnFilterCameraView.setFragShader(3);
                }
        );
        mBinding.btnFilter4.setOnClickListener(v -> {
                    mBinding.btnFilterCameraView.setFragShader(4);
                }
        );

    }
}