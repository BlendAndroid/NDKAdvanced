package com.blend.ndkadvanced.screenshare.push;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityPushScreenShareBinding;

public class PushScreenShareActivity extends AppCompatActivity {

    private ActivityPushScreenShareBinding mBinding;

    private PushSocketLive mPushSocketLive;

    private PushScreenShareService mPushScreenShareService;
    private ScreenShareServiceConnection mServiceConnection;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPushScreenShareBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.btnPushScreenShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceConnection = new ScreenShareServiceConnection();

                // 绑定服务
                Intent intent = new Intent();
                intent.setClass(PushScreenShareActivity.this, PushScreenShareService.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        });

        mBinding.btnStopPushScreenShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPushSocketLive.close();
                unbindService(mServiceConnection);
            }
        });
    }

    private final ActivityResultLauncher<Intent> mIntentResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                mMediaProjection = mMediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                if (mPushScreenShareService != null) {
                    mPushSocketLive = new PushSocketLive(12001);
                    // 开始Socket服务
                    mPushSocketLive.start();

                    mPushScreenShareService.init(mPushSocketLive, mMediaProjection);
                    mPushScreenShareService.startPush();
                }
            } else {
                Toast.makeText(mPushScreenShareService, "屏幕录制服务开启失败", Toast.LENGTH_SHORT).show();
            }
        }
    });


    private class ScreenShareServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof PushScreenShareService.PushScreenShareBinder) {
                // 拿到服务
                mPushScreenShareService = ((PushScreenShareService.PushScreenShareBinder) service).getService();

                // 获取权限
                mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                mIntentResult.launch(captureIntent);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }
}