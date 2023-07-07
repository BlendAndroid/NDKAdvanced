package com.blend.ndkadvanced.rtmp;

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

import com.blend.ndkadvanced.databinding.ActivityRtmpBinding;

public class RTMPActivity extends AppCompatActivity {

    private ActivityRtmpBinding mBinding;

    private MediaProjectionManager mediaProjectionManager;

    private MediaProjection mediaProjection;

    private ScreenLiveService mScreenShortService;

    private ScreenShortServiceConnection mServiceConnection;

    private static final String rtspHost = "rtmp://live-push.bilivideo.com/live-bvc/";
    private static final String streamKey = "?streamname=live_7207055_19149350&key=f12a84dbf19c61111ab813216c5dfcf1&schedule=rtmp&pflag=1";

    private static final String url = rtspHost + streamKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityRtmpBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.btnRTMPTOBibi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开启屏幕录制服务
                bindScreenService();
            }
        });

        mBinding.btnStopRTMPTOBibi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScreenShortService != null) {
                    mScreenShortService.stopLive();
                    unbindService(mServiceConnection);
                    mScreenShortService = null;
                }
            }
        });

    }


    private void bindScreenService() {
        mServiceConnection = new ScreenShortServiceConnection();

        Intent intent = new Intent();
        intent.setClass(this, ScreenLiveService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private class ScreenShortServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof ScreenLiveService.ScreenLiveBinder) {
                //录屏
                mScreenShortService = ((ScreenLiveService.ScreenLiveBinder) service).getService();

                // 获取权限
                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                mIntentResult.launch(captureIntent);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    private final ActivityResultLauncher<Intent> mIntentResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                if (mScreenShortService != null) {
                    mScreenShortService.startLive(url, mediaProjection);
                }
            } else {
                Toast.makeText(RTMPActivity.this, "屏幕录制服务开启失败", Toast.LENGTH_SHORT).show();
            }
        }
    });
}