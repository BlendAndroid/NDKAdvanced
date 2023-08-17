package com.blend.ndkadvanced.opengl.vary;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityVaryOpenGlactivityBinding;
import com.blend.ndkadvanced.opengl.picture.PictureFilterRender;

public class VaryOpenGLActivity extends AppCompatActivity {

    private ActivityVaryOpenGlactivityBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityVaryOpenGlactivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.varyGLView.setEGLContextClientVersion(2);
        mBinding.varyGLView.setRenderer(new VaryRender(this));
        mBinding.varyGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 生命周期对GLSurfaceView做处理
        mBinding.varyGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBinding.varyGLView.onPause();
    }
}