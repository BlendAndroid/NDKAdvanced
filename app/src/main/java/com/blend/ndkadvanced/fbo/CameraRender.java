package com.blend.ndkadvanced.fbo;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "david";
    private CameraHelper cameraHelper;
    private CameraSurfaceView cameraView;
    private SurfaceTexture mCameraTexure;
    private ScreenFilter recordFilter;
    private MediaRecorder mRecorder;
    private CameraFilter cameraFilter;
    private int[] textures;
    float[] mtx = new float[16];

    public CameraRender(CameraSurfaceView cameraView) {
        this.cameraView = cameraView;
        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
        //  打开摄像头
        cameraHelper = new CameraHelper(lifecycleOwner, this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        textures = new int[1];
        mCameraTexure.attachToGLContext(textures[0]);
        // 监听摄像头数据回调，
        mCameraTexure.setOnFrameAvailableListener(this);

        // 创建相机拍摄滤镜，并进行FBO离屏渲染
        cameraFilter = new CameraFilter(cameraView.getContext());

        // 创建相机显示滤镜
        recordFilter = new ScreenFilter(cameraView.getContext());

        File file = new File(cameraView.getContext().getExternalCacheDir(), "fboOutput.mp4");
        if (file.exists()) {
            file.delete();
        }

        String path = file.getAbsolutePath();
        mRecorder = new MediaRecorder(cameraView.getContext(), path, EGL14.eglGetCurrentContext(), 480, 640);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        cameraFilter.setSize(width, height);
        recordFilter.setSize(width, height);
    }

    // 在 Android 中，`onDrawFrame` 方法是在 GLSurfaceView 的渲染线程中执行的。当 GLSurfaceView 被创建后，
    // 它会创建一个专门用于渲染的线程，并在该线程中运行 OpenGL 相关的操作。这个专门的线程被称为渲染线程（Rendering Thread），
    // 它负责处理 GLSurfaceView 的渲染和事件分发等任务。
    @Override
    public void onDrawFrame(GL10 gl) {
        // 输出是 GLThread xxxx
        // Log.i(TAG, "线程: " + Thread.currentThread().getName());
        mCameraTexure.updateTexImage();
        mCameraTexure.getTransformMatrix(mtx);

        // 设置采样矩阵
        cameraFilter.setTransformMatrix(mtx);

        // 将帧数据写入FBO(帧缓存对象)中，并返回FBO对象对应的纹理id
        int id = cameraFilter.onDraw(textures[0]);

        // 使用FBO中的纹理id作为输入纹理id，做一次普通的渲染，绘制到屏幕上
        id = recordFilter.onDraw(id);

        // 使用FBO中的纹理id作为输入纹理id，作为MP4的输入源
        mRecorder.fireFrame(id, mCameraTexure.getTimestamp());
    }

    // Preview.OnPreviewOutputUpdateListener的回调方法,用于接收相机预览数据
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        // 摄像头预览到的数据 在这里
        mCameraTexure = output.getSurfaceTexture();
    }

    // 当有数据过来的时候回调
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        cameraView.requestRender();
    }

    public void startRecord(float speed) {
        try {
            mRecorder.start(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        mRecorder.stop();
    }
}
