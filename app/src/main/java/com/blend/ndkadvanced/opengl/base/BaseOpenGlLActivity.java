package com.blend.ndkadvanced.opengl.base;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityBaseOpenGlLactivityBinding;
import com.blend.ndkadvanced.opengl.picture.PictureRender;

public class BaseOpenGlLActivity extends AppCompatActivity {

    public static final String BASE_OPEN_GL_ACTIVITY = "BASE_OPEN_GL_ACTIVITY";

    private ActivityBaseOpenGlLactivityBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityBaseOpenGlLactivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        int type = getIntent().getExtras().getInt(BASE_OPEN_GL_ACTIVITY);
        // setEGLContextClientVersion ()设置OpenGL的版本，如果设置为2，则表示使用OpenGL2.0的渲染接口
        mBinding.baseGlSurfaceView.setEGLContextClientVersion(2);
        // 设置渲染器
        switch (type) {
            case 0:
                mBinding.baseGlSurfaceView.setRenderer(new TriangleRender());
                break;
            case 1:
                mBinding.baseGlSurfaceView.setRenderer(new TriangleWithCameraRender());
                break;
            case 2:
                mBinding.baseGlSurfaceView.setRenderer(new TriangleColorRender());
                break;
            case 3:
                mBinding.baseGlSurfaceView.setRenderer(new SquareRender());  // 正方形
                break;
            case 4:
                mBinding.baseGlSurfaceView.setRenderer(new OvalRender());    // 圆形
                break;
            case 5:
                mBinding.baseGlSurfaceView.setRenderer(new CubeRender());    // 立方体
                break;
            case 6:
                mBinding.baseGlSurfaceView.setRenderer(new ConeRender(this));   //圆锥
                break;
            case 7:
                mBinding.baseGlSurfaceView.setRenderer(new PictureRender(this));    //纹理贴图之显示图片
                break;
        }
        /*渲染方式，RENDERMODE_WHEN_DIRTY表示被动渲染，只有在调用requestRender或者onResume等方法时才会进行渲染。RENDERMODE_CONTINUOUSLY表示持续渲染*/
        // 设置渲染模式为渲染模式为RENDERMODE_WHEN_DIRTY
        mBinding.baseGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 生命周期对GLSurfaceView做处理
        mBinding.baseGlSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBinding.baseGlSurfaceView.onPause();
    }
}