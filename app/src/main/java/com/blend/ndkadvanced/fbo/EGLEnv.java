package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

/**
 * 为什么需要这个EGL环境：因为最终的纹理对象是在GPU的，而编码需要dsp芯片进行编码
 * 所以就需要这个类，用于从GPU中拿数据，然后传递给dsp芯片进行编码
 * <p>
 * EGL是OpenGL渲染和本地窗口系统(Windows系统的Window，Android中的SurfaceView等)
 * 之间的一个中间接口层。引入EGL就是为了屏蔽不同平台上不同窗口的区别。
 * <p>
 * OpenGL 是一个操作 GPU 的 API，它通过驱动向 GPU 发送相关指令，控制图形渲染管线状态机的运行状态，但是当涉及到
 * 与本地窗口系统进行交互时，就需要这么一个中间层，且它最好是与平台无关的, 因此 EGL 被设计出来，作为 OpenGL 和原生窗口系统之间的桥梁。
 */
public class EGLEnv {
    private final EGLDisplay mEglDisplay;
    // 调用 oepngl 的函数
    private final EGLContext mEglContext;
    private final EGLSurface mEglSurface;

    private AbstractFilter screenFilter;

    // mediacodec  提供的场地
    public EGLEnv(Context context, EGLContext mGlContext, Surface surface, int width, int height) {
        // 获得显示窗口，作为OpenGL的绘制目标
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        //初始化显示设备，主次版本号
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }


        // 配置属性选项
        // 指定一些配置项，例如色彩格式、像素格式、RGBA的表示以及SurfaceType等，实际上也就是指FrameBuffer的配置参数。
        int[] configAttribs = {EGL14.EGL_RED_SIZE, 8, //颜色缓冲区中红色位数
                EGL14.EGL_GREEN_SIZE, 8,//颜色缓冲区中绿色位数
                EGL14.EGL_BLUE_SIZE, 8, //
                EGL14.EGL_ALPHA_SIZE, 8,//
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //opengl es 2.0
                EGL14.EGL_NONE};
        int[] numConfigs = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        //EGL 根据属性选择一个配置
        if (!EGL14.eglChooseConfig(mEglDisplay, configAttribs, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }

        // 创建EGLContext
        EGLConfig eglConfig = configs[0];
        int[] context_attrib_list = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        // 一个线程对应一个EGLContext
        // 第三个参数即为需要共享的上下文对象，资源包括纹理、FrameBuffer以及其他的Buffer等资源
        mEglContext = EGL14.eglCreateContext(mEglDisplay, eglConfig, mGlContext, context_attrib_list, 0);
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }

        // 创建EGLSurface，把EGL和渲染的目标连接起来
        int[] surface_attrib_list = {EGL14.EGL_NONE};
        // EGL14.eglCreatePbufferSurface：创建离屏的Surface
        // EGL14.eglCreateWindowSurface：创建可以实际显示的Surface
        // 非离屏 需要surface承载，这个surface就是MediaCodec创建的Surface
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, eglConfig, surface, surface_attrib_list, 0);
        if (mEglSurface == null) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }
        /**
         * 绑定当前线程的显示器display mEglDisplay  虚拟物理设备
         *
         * 就像屏幕共享的时候，也是向虚拟屏幕进行渲染的
         */
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("EGL error " + EGL14.eglGetError());
        }
        // 进行一次普通的渲染，向虚拟屏幕上渲染,在这里设置的滤镜,是用于保存mp4视频的
        screenFilter = new SplitFilter(context);
        screenFilter.setSize(width, height);
    }

    public void draw(int textureId, long timestamp) {
        // 画到虚拟屏幕上，就是向MediaCodec创建的Surface上进行渲染
        // 就像屏幕共享的时候，也是向虚拟屏幕进行渲染的
        screenFilter.onDraw(textureId);
        // 给帧缓冲时间戳，刷新eglsurface的时间戳
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, timestamp);

        // EGL在初始化时默认设置的是双缓冲模式，也就是两份FrameBuffer;
        // 一个用于绘制图像，一个用于显示图像，每次绘制完一帧都需要交换一次缓冲。
        // 因此需要在OpenGL ES绘制完毕后，调用如下进行交换
        // 当EGL将一个fb 显示屏幕上，另一个就在后台等待opengl进行交换
        EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
    }

    public void release() {
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        EGL14.eglReleaseThread();
        EGL14.eglTerminate(mEglDisplay);
        screenFilter.release();
    }
}
