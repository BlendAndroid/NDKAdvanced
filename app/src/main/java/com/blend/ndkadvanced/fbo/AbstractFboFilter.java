package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.opengl.GLES20;

// 所有的渲染操作将会渲染到当前绑定帧缓冲的附件（纹理对象）中。由于帧缓冲不是默认帧缓冲，渲染指令将不会对窗口的视觉输出有任何影响。
// 出于这个原因，渲染到一个不同的帧缓冲被叫做离屏渲染(Off-screen Rendering)。
public abstract class AbstractFboFilter extends AbstractFilter {
    // fbo对象
    int[] frameBuffer;
    // 纹理对象
    int[] frameTextures;

    public AbstractFboFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        super(context, vertexShaderId, fragmentShaderId);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        releaseFrame();
        // 下面是FBO的设置

        // 让摄像头的数据先渲染到fbo
        frameBuffer = new int[1];
        // 用于生成帧缓冲对象（Framebuffer Object，FBO）的ID。
        // 帧缓冲对象是用于将渲染操作输出到的缓冲区，它可以包含颜色缓冲区、深度缓冲区、模板缓冲区等。在使用帧缓冲对象之前，需要生成一个唯一的ID来标识该对象
        GLES20.glGenFramebuffers(1, frameBuffer, 0);

        // 为帧缓冲创建一个纹理对象
        // 用于存储生成的纹理对象
        frameTextures = new int[1];

        // 生成纹理对象
        // 用于存储和处理图像数据的OpenGL对象，可以用于将图像映射到几何图元上进行纹理贴图
        GLES20.glGenTextures(frameTextures.length, frameTextures, 0);
        // 配置纹理
        for (int i = 0; i < frameTextures.length; i++) {
            // 纹理单元GL_TEXTURE0默认总是被激活
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0); // 在绑定纹理之前先激活纹理单元
            // 开始配置纹理，先将纹理对象绑定到当前活动的纹理单元
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTextures[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);//放大过滤
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);//缩小过滤
            // 配置纹理结束，解除纹理句柄绑定
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        // 将新建的纹理和纹理单元绑定起来
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTextures[0]);
        /**
         * 指定一个二维的纹理图片
         * level
         *     指定细节级别，0级表示基本图像，n级则表示Mipmap缩小n级之后的图像（缩小2^n）
         * internalformat
         *     指定纹理内部格式，必须是下列符号常量之一：GL_ALPHA，GL_LUMINANCE，GL_LUMINANCE_ALPHA，GL_RGB，GL_RGBA。
         * width height
         *     指定纹理图像的宽高，所有实现都支持宽高至少为64 纹素的2D纹理图像和宽高至少为16 纹素的立方体贴图纹理图像 。
         * border
         *     指定边框的宽度。必须为0。
         * format
         *     指定纹理数据的格式。必须匹配internalformat。下面的符号值被接受：GL_ALPHA，GL_RGB，GL_RGBA，GL_LUMINANCE，和GL_LUMINANCE_ALPHA。
         * type
         *     指定纹理数据的数据类型。下面的符号值被接受：GL_UNSIGNED_BYTE，GL_UNSIGNED_SHORT_5_6_5，GL_UNSIGNED_SHORT_4_4_4_4，和GL_UNSIGNED_SHORT_5_5_5_1。
         * data
         *     指定一个指向内存中图像数据的指针。
         */
        // 这里传的data是null，对于这个纹理，仅仅分配了内存而没有填充它。填充这个纹理将会在我们渲染到帧缓冲之后来进行。
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // 将帧缓冲对象（Framebuffer Object，FBO）绑定到OpenGL上下文中，使得所有的渲染操作都会输出到该帧缓冲对象上
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);  //绑定FBO

        // glFrameBufferTexture2D有以下的参数：
        // target：帧缓冲的目标（绘制、读取或者两者皆有）
        // attachment：我们想要附加的附件类型。当前我们正在附加一个颜色附件。注意最后的0意味着我们可以附加多个颜色附件。
        // textarget：你希望附加的纹理类型
        // texture：要附加的纹理本身
        // level：多级渐远纹理的级别。我们将它保留为0。
        // 真正发生绑定，将创建好的纹理对象，附加到帧缓冲对象上
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
                frameTextures[0], 0);

        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        // 解绑FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    private void releaseFrame() {
        if (frameTextures != null) {
            GLES20.glDeleteTextures(1, frameTextures, 0);
            frameTextures = null;
        }

        if (frameBuffer != null) {
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
        }
    }

    @Override
    public int onDraw(int texture) {
        // 绑定FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        // 也就是再走一遍渲染的流程，选定离屏渲染的 Program，绑定 VAO 和图像纹理
        // 进行绘制（离屏渲染）
        super.onDraw(texture);
        // 解绑FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return frameTextures[0];
    }

    @Override
    public void beforeDraw() {

    }

}
