package com.blend.ndkadvanced.x264;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityX264Binding;

public class X264AndFaacActivity extends AppCompatActivity {

    private ActivityX264Binding mBinding;
    private LivePusher livePusher;
    private CameraXHelper mCameraXHelper;
    private AudioChannel audioChannel;

    private static final String rtspHost = "rtmp://live-push.bilivideo.com/live-bvc/";
    private static final String streamKey = "?streamname=live_7207055_19149350&key=f12a84dbf19c61111ab813216c5dfcf1&schedule=rtmp&pflag=1";

    private static final String url = rtspHost + streamKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityX264Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        livePusher = new LivePusher();

        // 初始化视频
        mCameraXHelper = new CameraXHelper(this, mBinding.x264TextureView, livePusher);
        // 初始化音频
        audioChannel = new AudioChannel(44100, 2, livePusher);

        // 链接B站
        livePusher.startLive(url);

        mBinding.btnToggleCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mBinding.btnStartLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraXHelper.startLive();
                audioChannel.start();
            }
        });

        mBinding.btnStopLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraXHelper.stopLive();
                audioChannel.stop();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        livePusher.native_release();
    }
}