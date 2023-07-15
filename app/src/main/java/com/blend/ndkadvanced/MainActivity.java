package com.blend.ndkadvanced;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.blend.ndkadvanced.audio.AudioActivity;
import com.blend.ndkadvanced.camera1.Camera1Activity;
import com.blend.ndkadvanced.camerax.CameraXActivity;
import com.blend.ndkadvanced.databinding.ActivityMainBinding;
import com.blend.ndkadvanced.filter.CameraFilterActivity;
import com.blend.ndkadvanced.gif.GifDemoActivity;
import com.blend.ndkadvanced.golomb.GolombActivity;
import com.blend.ndkadvanced.h264.H264Activity;
import com.blend.ndkadvanced.hello.HelloWorldActivity;
import com.blend.ndkadvanced.opengl.OpenGLActivity;
import com.blend.ndkadvanced.rtmp.RTMPActivity;
import com.blend.ndkadvanced.screenshare.player.PlayerScreenShareActivity;
import com.blend.ndkadvanced.screenshare.push.PushScreenShareActivity;
import com.blend.ndkadvanced.videochat.player.PlayerVideoChatActivity;
import com.blend.ndkadvanced.videochat.push.PushVideoChatActivity;
import com.blend.ndkadvanced.x264.X264AndFaacActivity;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

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

        binding.btnGolomb.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GolombActivity.class)));

        binding.btnCamera1.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, Camera1Activity.class)));

        binding.btnScreenSharePush.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PushScreenShareActivity.class)));

        binding.btnScreenSharePlayer.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PlayerScreenShareActivity.class)));

        binding.btnVideoChatPush.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PushVideoChatActivity.class)));

        binding.btnVideoChatPlayer.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, PlayerVideoChatActivity.class)));

        binding.btnAudio.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AudioActivity.class)));

        binding.btnRTMP.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RTMPActivity.class));
        });

        binding.btnX264.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, X264AndFaacActivity.class));
        });

        binding.btnCameraX.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CameraXActivity.class));
        });

        binding.btnOpenGL.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, OpenGLActivity.class));
                }
        );

        binding.btnFilter.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, CameraFilterActivity.class));
                }
        );
    }

    public void verifyStoragePermissions() {
        int REQUEST_EXTERNAL_STORAGE = 1;

        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}