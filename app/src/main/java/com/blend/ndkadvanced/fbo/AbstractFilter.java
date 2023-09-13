package com.blend.ndkadvanced.fbo;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;

import android.content.Context;
import android.opengl.GLES20;

import com.blend.ndkadvanced.utils.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class AbstractFilter {

    public int program;

    private int vPosition;
    FloatBuffer textureBuffer; // 纹理坐标
    private int vCoord;
    private int vTexture;
    private int mWidth;
    private int mHeight;
    FloatBuffer vertexBuffer; //gpu顶点缓冲区
    float[] VERTEX = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };

    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    public AbstractFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        vertexBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);
        textureBuffer.position(0);

        String vertexSharder = OpenGLUtils.readRawTextFile(context, vertexShaderId);
        String fragSharder = OpenGLUtils.readRawTextFile(context, fragmentShaderId);
        program = OpenGLUtils.loadProgram(vertexSharder, fragSharder);

        vPosition = GLES20.glGetAttribLocation(program, "vPosition");
        // 接收纹理坐标，接收采样器采样图片的坐标
        vCoord = GLES20.glGetAttribLocation(program, "vCoord");
        //采样点的坐标
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    //获取到摄像头纹理数据,开始渲染
    public int onDraw(int texture) {
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glUseProgram(program);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(vCoord);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(vTexture, 0);

        // 模板方法
        beforeDraw();
        // 通知渲染画面到屏幕
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        // 将纹理与纹理单元解除绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return texture;
    }

    public abstract void beforeDraw();

    public void release() {
        GLES20.glDeleteProgram(program);
    }
}
