package com.blend.ndkadvanced.opengl.vary;

import android.content.Context;
import android.opengl.GLES20;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.utils.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * Created by wuwang on 2016/10/30
 */

class Cube {

    final float cubePositions[] = {-1.0f, 1.0f, 1.0f,    //正面左上0
            -1.0f, -1.0f, 1.0f,   //正面左下1
            1.0f, -1.0f, 1.0f,    //正面右下2
            1.0f, 1.0f, 1.0f,     //正面右上3
            -1.0f, 1.0f, -1.0f,    //反面左上4
            -1.0f, -1.0f, -1.0f,   //反面左下5
            1.0f, -1.0f, -1.0f,    //反面右下6
            1.0f, 1.0f, -1.0f,     //反面右上7
    };
    final short index[] = {6, 7, 4, 6, 4, 5,    //后面
            6, 3, 7, 6, 2, 3,    //右面
            6, 5, 1, 6, 1, 2,    //下面
            0, 3, 2, 0, 2, 1,    //正面
            0, 1, 5, 0, 5, 4,    //左面
            0, 7, 3, 0, 4, 7,    //上面
    };

    float color[] = {0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f,};

    private FloatBuffer vertexBuf, colorBuf;
    private ShortBuffer indexBuf;
    private Context context;
    private int mProgram;
    private int hVertex;
    private int hColor;
    private int hMatrix;

    public Cube(Context context) {
        this.context = context;
        initData();
    }

    private void initData() {
        ByteBuffer a = ByteBuffer.allocateDirect(cubePositions.length * 4);
        a.order(ByteOrder.nativeOrder());
        vertexBuf = a.asFloatBuffer();
        vertexBuf.put(cubePositions);
        vertexBuf.position(0);
        ByteBuffer b = ByteBuffer.allocateDirect(color.length * 4);
        b.order(ByteOrder.nativeOrder());
        colorBuf = b.asFloatBuffer();
        colorBuf.put(color);
        colorBuf.position(0);
        ByteBuffer c = ByteBuffer.allocateDirect(index.length * 2);
        c.order(ByteOrder.nativeOrder());
        indexBuf = c.asShortBuffer();
        indexBuf.put(index);
        indexBuf.position(0);
    }

    public void create() {
        String vertexShaderString = OpenGLUtils.readRawTextFile(context, R.raw.default_vert);
        String fragShaderString = OpenGLUtils.readRawTextFile(context, R.raw.default_frag);

        // 加载顶点着色器和片元着色器的Shader代码
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderString);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderString);

        //创建一个空的OpenGLES程序
        mProgram = GLES20.glCreateProgram();
        //将顶点着色器加入到程序
        GLES20.glAttachShader(mProgram, vertexShader);
        //将片元着色器加入到程序中
        GLES20.glAttachShader(mProgram, fragmentShader);
        //连接到着色器程序
        GLES20.glLinkProgram(mProgram);

        hVertex = GLES20.glGetAttribLocation(mProgram, "vPosition");
        hColor = GLES20.glGetAttribLocation(mProgram, "aColor");
        hMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
    }

    private int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器或者片元着色器
        int shader = GLES20.glCreateShader(type);
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private float[] matrix;

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    public void drawSelf() {

        //将程序加入到OpenGLES2.0环境
        GLES20.glUseProgram(mProgram);
        //指定vMatrix的值
        if (matrix != null) {
            GLES20.glUniformMatrix4fv(hMatrix, 1, false, matrix, 0);
        }
        //启用句柄
        GLES20.glEnableVertexAttribArray(hVertex);
        GLES20.glEnableVertexAttribArray(hColor);
        //准备三角形的坐标数据
        GLES20.glVertexAttribPointer(hVertex, 3, GLES20.GL_FLOAT, false, 0, vertexBuf);
        //设置绘制三角形的颜色
        GLES20.glVertexAttribPointer(hColor, 4, GLES20.GL_FLOAT, false, 0, colorBuf);
        //索引法绘制正方体
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, index.length, GLES20.GL_UNSIGNED_SHORT, indexBuf);
        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(hVertex);
        GLES20.glDisableVertexAttribArray(hColor);
    }

}
