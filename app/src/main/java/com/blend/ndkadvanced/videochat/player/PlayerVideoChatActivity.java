package com.blend.ndkadvanced.videochat.player;

import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityPlayerVideoChatBinding;
import com.blend.ndkadvanced.socket.PlayerSocketLive;
import com.blend.ndkadvanced.socket.PushSocketLive;
import com.blend.ndkadvanced.socket.SocketCallback;
import com.blend.ndkadvanced.videochat.DecoderPlayerLiveH265;
import com.blend.ndkadvanced.videochat.push.PushVideoChatActivity;

public class PlayerVideoChatActivity extends AppCompatActivity implements SocketCallback {

    private ActivityPlayerVideoChatBinding mBinding;

    private DecoderPlayerLiveH265 decoderPlayerLiveH265;
    private Surface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPlayerVideoChatBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // 获取远端的数据,需要解码
        mBinding.remotePlayerSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                decoderPlayerLiveH265 = new DecoderPlayerLiveH265();
                decoderPlayerLiveH265.initDecoder(surface);

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });

        // 本地摄像头,开始解码
        mBinding.btnPlayerVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerSocketLive pushSocketLive = new PlayerSocketLive(PlayerVideoChatActivity.this);
                mBinding.localPlayerSurfaceView.startCapture(pushSocketLive);
            }
        });
    }

    @Override
    public void callBack(byte[] data) {
        if (decoderPlayerLiveH265 != null) {
            decoderPlayerLiveH265.callBack(data);
        }
    }
}