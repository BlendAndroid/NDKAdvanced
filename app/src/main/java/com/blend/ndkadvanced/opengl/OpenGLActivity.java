package com.blend.ndkadvanced.opengl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityOpenGlBinding;

public class OpenGLActivity extends AppCompatActivity {

    private ActivityOpenGlBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityOpenGlBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());


        // setEGLContextClientVersion ()设置OpenGL的版本，如果设置为2，则表示使用OpenGL2.0的渲染接口
        mBinding.glSurfaceView.setEGLContextClientVersion(2);
        // 设置渲染器
        // mBinding.glSurfaceView.setRenderer(new TriangleRender());
        // mBinding.glSurfaceView.setRenderer(new TriangleWithCameraRender());
        // mBinding.glSurfaceView.setRenderer(new TriangleColorRender());
        // mBinding.glSurfaceView.setRenderer(new SquareRender());  // 正方形
        // mBinding.glSurfaceView.setRenderer(new OvalRender());    // 圆形
        // mBinding.glSurfaceView.setRenderer(new CubeRender());    // 立方体
        // mBinding.glSurfaceView.setRenderer(new ConeRender(this));   //圆锥
        mBinding.glSurfaceView.setRenderer(new PictureRender(this));    //纹理贴图之显示图片

        /*渲染方式，RENDERMODE_WHEN_DIRTY表示被动渲染，只有在调用requestRender或者onResume等方法时才会进行渲染。RENDERMODE_CONTINUOUSLY表示持续渲染*/
        // 设置渲染模式为渲染模式为RENDERMODE_WHEN_DIRTY
        mBinding.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mBinding.btnOpenGLChange.setOnClickListener(v -> {


        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 生命周期对GLSurfaceView做处理
        mBinding.glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBinding.glSurfaceView.onPause();
    }
}