package com.blend.ndkadvanced.h264;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityH264Binding;

import java.io.File;

public class H264Activity extends AppCompatActivity {

    private static final int MEDIA_PROJECTION_MANAGER = 100;

    private ActivityH264Binding mBinding;

    private H264Player mH264Player;

    private MediaProjectionManager mediaProjectionManager;

    private MediaProjection mediaProjection;

    private ScreenShortService mScreenShortService;

    private ScreenShortServiceConnection mServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityH264Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // 编码视频
        encoder();

        // 编码视频
        decoder();
    }

    private void encoder() {
        mBinding.h264Encoder.setOnClickListener(v -> {
            // 开启屏幕录制服务
            bindScreenService();
        });

        mBinding.h264EncoderClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 结束屏幕录制
                mScreenShortService.stopRecord();
                unbindService(mServiceConnection);
            }
        });
    }

    private void bindScreenService() {
        mServiceConnection = new ScreenShortServiceConnection();

        Intent intent = new Intent();
        intent.setClass(this, ScreenShortService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void decoder() {
        final SurfaceHolder surfaceHolder = mBinding.h264PlayerPreview.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                // 在surfaceCreated中获取创建好的Surface, 然后初始化H264Player
                mH264Player = new H264Player(H264Activity.this,
                        new File(Environment.getExternalStorageDirectory(), "out.h264").getAbsolutePath(),
                        surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

        mBinding.h264Decoder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mH264Player.play(true);
            }
        });

        mBinding.h264DecoderBitmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mH264Player.setImageView(mBinding.h264PlayerImage);
                mH264Player.play(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEDIA_PROJECTION_MANAGER && resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection
                    (resultCode, data);
            if (mScreenShortService != null) {
                mScreenShortService.setMediaProjection(mediaProjection);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
        super.onDestroy();
    }

    private class ScreenShortServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof ScreenShortService.ScreenShortBinder) {
                //录屏
                mScreenShortService = ((ScreenShortService.ScreenShortBinder) service).getService();

                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, MEDIA_PROJECTION_MANAGER);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }
}