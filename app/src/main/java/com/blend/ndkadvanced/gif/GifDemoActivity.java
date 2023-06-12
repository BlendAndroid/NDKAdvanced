package com.blend.ndkadvanced.gif;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.databinding.ActivityGifDemoBinding;

import java.io.File;

public class GifDemoActivity extends AppCompatActivity {

    private static final String TAG = "GifDemoActivity";

    private ActivityGifDemoBinding mBinding;
    private GifHandler mGifHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_demo);
        mBinding = ActivityGifDemoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        verifyStoragePermissions();
        mBinding.gifImageBtn.setOnClickListener(v -> ndkLoadGif());
    }

    public void verifyStoragePermissions() {
        int REQUEST_EXTERNAL_STORAGE = 1;

        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(GifDemoActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(GifDemoActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ndkLoadGif() {
        File file = new File(Environment.getExternalStorageDirectory(), "earth_night.gif");
        mGifHandler = GifHandler.load(file.getAbsolutePath());
        // int width = mGifHandler.getWidth();
        // int height = mGifHandler.getHeight();
        // Log.i("BlendAndroid", "宽: " + width + "   高: " + height);
    }
}