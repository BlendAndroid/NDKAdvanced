package com.blend.ndkadvanced.filter;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraRender";
    private CameraHelper cameraHelper;
    private CameraView cameraView;
    private SurfaceTexture mCameraTexture;

    //    int
    private ScreenFilter screenFilter;
    private int[] textures;
    float[] mtx = new float[16];

    private int width;
    private int height;

    private int type;

    private volatile boolean mFilterChange = false;

    public CameraRender(CameraView cameraView) {
        this.cameraView = cameraView;
        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
//        打开摄像头
        cameraHelper = new CameraHelper(lifecycleOwner, this);
    }

    //textures
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 一个整型数组，用于存储生成的纹理对象的名称
        textures = new int[1];
        // 让 SurfaceTexture 与 Gpu  共享一个数据源,也就是开启一个图层,共用一个图层,这个图层的范围是  0-31
        // 指将一个 OpenGL 对象（如纹理、缓冲区、着色器程序等）与当前的 OpenGL 上下文进行关联，以便进行后续的渲染和操作。
        mCameraTexture.attachToGLContext(textures[0]);
        // 监听摄像头数据回调，
        mCameraTexture.setOnFrameAvailableListener(this);
        screenFilter = new ScreenFilter(cameraView.getContext(), 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        screenFilter.setSize(width, height);
    }

    //  有数据的时候给,然后进行渲染
    // 在 Android 中，`onDrawFrame` 方法是在 GLSurfaceView 的渲染线程中执行的。当 GLSurfaceView 被创建后，
    // 它会创建一个专门用于渲染的线程，并在该线程中运行 OpenGL 相关的操作。这个专门的线程被称为渲染线程（Rendering Thread），
    // 它负责处理 GLSurfaceView 的渲染和事件分发等任务。
    @Override
    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "线程: " + Thread.currentThread().getName());

        if (mFilterChange) {
            screenFilter = new ScreenFilter(cameraView.getContext(), type);
            screenFilter.setSize(width, height);
            mFilterChange = false;
        }

//        摄像头的数据  ---》
//        更新摄像头的数据给gpu
        mCameraTexture.updateTexImage();

        //  获取到矩阵
        mCameraTexture.getTransformMatrix(mtx);

        screenFilter.setTransformMatrix(mtx);
        // 设置摄像头数据
        screenFilter.onDraw(textures[0]);
    }

    //
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
//        摄像头预览到的数据 在这里
        mCameraTexture = output.getSurfaceTexture();
    }

    //当有数据 过来的时候
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //一帧 一帧回调时, 手动触发onDrawFrame, 让  gpu  进行绘制
        cameraView.requestRender();
    }

    public void setFragShader(int type) {
        this.type = type;
        mFilterChange = true;
    }
}
