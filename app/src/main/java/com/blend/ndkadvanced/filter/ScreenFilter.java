package com.blend.ndkadvanced.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.utils.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;

public class ScreenFilter {

    private static final String TAG = "ScreenFilter";

    //    顶点着色器
//    片元着色器
    private int program;
    //句柄  gpu中  vPosition
    private final int vPosition;
    private final FloatBuffer textureBuffer; // 纹理坐标
    private final int vCoord;
    private final int vTexture;
    private final int vMatrix;
    private int mWidth;
    private int mHeight;
    private float[] mtx;

    private String vertexShader;

    private String fragShader;

    //gpu顶点缓冲区，把cpu的数据，通过这个缓冲区给到GPU
    private final FloatBuffer vertexBuffer; //顶点坐标缓存区

    // 这个纹理坐标缓冲区,在CPU中设置,vPosotion在GPU中设置，两边对应，需要设置桥梁
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

    public ScreenFilter(Context context, int type) {
        // 先申请4个坐标，每个坐标4个字节（vec4），每个坐标含有xy两个，再排序
        vertexBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);

        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);

//      读取顶点着色器
        vertexShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_vert);
//      读取片元着色器
        if (type == 0) {
            fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag);
        } else if (type == 1) {
            fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag1);
        } else if (type == 2) {
            fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag2);
        } else if (type == 3) {
            fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag3);
        } else if (type == 4) {
            fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag4);
        }
        // 是一个int值,是一个native的索引,对GPU有用
        program = OpenGLUtils.loadProgram(vertexShader, fragShader);

        // 获取底层vPosition的地址,也就是gpu中vPosition的句柄
        vPosition = GLES20.glGetAttribLocation(program, "vPosition");//0
        //接收纹理坐标，接收采样器采样图片的坐标
        vCoord = GLES20.glGetAttribLocation(program, "vCoord");//1
        //采样点的坐标
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
        //变换矩阵， 需要将原本的vCoord（01,11,00,10） 与矩阵相乘
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }

    //获取到摄像头数据,开始渲染
    public void onDraw(int texture) {
//设置View 的大小
        GLES20.glViewport(0, 0, mWidth, mHeight);
//        使用程序
        GLES20.glUseProgram(program);
//        从索引为0的地方读
        vertexBuffer.position(0);
        // index:指定要修改的通用顶点属性的索引。
        // size:指定每个通用顶点属性的组件个数
        // type:指定数组中每个组件的数据类型。
        // 接受符号常量GL_FLOAT  GL_BYTE，GL_UNSIGNED_BYTE，GL_SHORT，GL_UNSIGNED_SHORT或GL_FIXED。 初始值为GL_FLOAT。
        // normalized:指定在访问定点数据值时是应将其标准化（GL_TRUE）还是直接转换为定点值（GL_FALSE）。
        // 传递数据给vPosition
        GLES20.glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer);
        // 生效
        GLES20.glEnableVertexAttribArray(vPosition);


        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);

        // ----------以上代码,形状就确定了------------

        //激活图层,这里是第0个图层,获取GPU的数据信息
        GLES20.glActiveTexture(GL_TEXTURE0);

        //生成一个采样器, 从texture采样
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(vTexture, GL_TEXTURE0);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
        //通知渲染器
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }


}
