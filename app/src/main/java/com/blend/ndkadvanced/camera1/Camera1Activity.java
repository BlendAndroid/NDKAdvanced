package com.blend.ndkadvanced.camera1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.blend.ndkadvanced.databinding.ActivityCamera1Binding;

public class Camera1Activity extends AppCompatActivity {

    private ActivityCamera1Binding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCamera1Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBinding.btnCamera1Capture.setOnClickListener(v -> {
                    mBinding.SurfaceCamera1.startCapture();
                }
        );

        verifyCameraPermissions();
    }

    public void verifyCameraPermissions() {
        int REQUEST_EXTERNAL_STORAGE = 1;

        String[] PERMISSIONS_CAMERA = {
                Manifest.permission.CAMERA};
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(Camera1Activity.this,
                    Manifest.permission.CAMERA);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(Camera1Activity.this, PERMISSIONS_CAMERA, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}