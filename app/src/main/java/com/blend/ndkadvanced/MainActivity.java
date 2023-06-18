package com.blend.ndkadvanced;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.blend.ndkadvanced.databinding.ActivityMainBinding;
import com.blend.ndkadvanced.gif.GifDemoActivity;
import com.blend.ndkadvanced.h264.H264Activity;
import com.blend.ndkadvanced.hello.HelloWorldActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        verifyStoragePermissions();

        binding.btnHelloWorld.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HelloWorldActivity.class))
        );

        binding.btnGif.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GifDemoActivity.class))
        );

        binding.btnH264.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, H264Activity.class)));

    }

    public void verifyStoragePermissions() {
        int REQUEST_EXTERNAL_STORAGE = 1;

        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}