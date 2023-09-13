package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class CameraSurfaceView extends GLSurfaceView {
    private CameraRender renderer;

    public CameraSurfaceView(Context context) {
        super(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new CameraRender(this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private Speed mSpeed = Speed.MODE_NORMAL;

    public enum Speed {
        MODE_EXTRA_SLOW, MODE_SLOW, MODE_NORMAL, MODE_FAST, MODE_EXTRA_FAST
    }

    public void setSpeed(Speed speed) {
        this.mSpeed = speed;
    }

    /**
     * 速度  时间/速度 speed小于就是放慢 大于1就是加快
     */
    public void startRecord() {
        float speed = 1.f;
        switch (mSpeed) {
            case MODE_EXTRA_SLOW:
                speed = 0.3f;
                break;
            case MODE_SLOW:
                speed = 0.5f;
                break;
            case MODE_NORMAL:
                speed = 1.f;
                break;
            case MODE_FAST:
                speed = 2.f;
                break;
            case MODE_EXTRA_FAST:
                speed = 3.f;
                break;
        }
        renderer.startRecord(speed);
    }

    public void stopRecord() {
        renderer.stopRecord();
    }
}
