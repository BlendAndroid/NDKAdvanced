package com.blend.ndkadvanced.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// 彩色三角形，需要在不同的片元赋值不同的颜色
/*
public static void glVertexAttribPointer(int indx, int size, int type,
                                          boolean normalized, int stride, Buffer ptr)

参数说明：

- `indx`：指定要修改的顶点属性的索引，从 0 开始。
- `size`：指定每个顶点属性的组成数量，例如 2 表示顶点属性由两个浮点数组成，通常是 2、3 或 4。
- `type`：指定每个组成部分的数据类型，通常是 `GL_FLOAT`。
- `normalized`：指定是否将非浮点型数据规范化到范围 [-1.0, 1.0] 或 [0, 1]，一般设置为 false。
- `stride`：指定数组中相邻两个顶点之间的字节数，通常是每组数据的长度乘以数据类型的字节数。
- `ptr`：指定存储顶点属性数据的缓冲区对象。

设置颜色的时候，决定了 OpenGL ES 如何解释顶点数据的排列方式。如果 `stride` 参数为 0，那么 OpenGL ES
就会认为顶点数据是紧密排列的，即每个顶点之间没有间隙，而是相邻的顶点数据紧密相连，这种方式叫做顶点数据紧密排列。
对于颜色属性数据来说，通常是在顶点数据的最后一个位置，因此不需要使用 `stride` 参数来指定颜色数据的间隙大小，
直接将 `stride` 参数设置为 0 即可。
 */
public class TriangleColorRender implements GLSurfaceView.Renderer {

    private int mProgram;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    // 增加了一个aColor（顶点的颜色）作为输入量，传递给了vColor
    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "uniform mat4 vMatrix;" +
                    "varying  vec4 vColor;" +
                    "attribute vec4 aColor;" +
                    "void main() {" +
                    "  gl_Position = vMatrix*vPosition;" +
                    "  vColor=aColor;" +
                    "}";

    // 片元着色器代码，就是Fragment Shader
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    // 这里定义为16,是因为是一个4 * 4的矩阵
    private float[] mProjectMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];


    private static float[] triangleCoords = {
            0.5f, 0.5f, 0.0f, // top
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f  // bottom right
    };

    // 每个顶点的个数
    static final int COORDS_PER_VERTEX = 3;
    // 每个颜色的个数,argb
    static final int COLOR = 4;

    //顶点个数
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    //顶点之间的偏移量
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节

    //设置颜色，依次为红绿蓝和透明通道
    private float color[] = {
            0.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f
    };

    private int mPositionHandle;
    private int mColorHandle;
    private int mMatrixHandle;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置清屏的颜色， 在使用OpenGL时，很多地方采用的参数变化范围都是从0到1
        GLES20.glClearColor(1f, 1f, 1f, 1.0f);

        //申请底层空间，先初始化buffer，数组的长度*4，因为一个float占4个字节
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer，用以传入OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer();
        //将给定float[]数据从当前位置开始，依次写入此缓冲区
        vertexBuffer.put(triangleCoords);
        //设置此缓冲区的位置。如果标记已定义并且大于新的位置，则要丢弃该标记。
        vertexBuffer.position(0);

        // 定义颜色
        ByteBuffer dd = ByteBuffer.allocateDirect(color.length * 4);
        dd.order(ByteOrder.nativeOrder());
        colorBuffer = dd.asFloatBuffer();
        colorBuffer.put(color);
        colorBuffer.position(0);


        // 加载顶点着色器和片元着色器的Shader代码
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        //创建一个空的OpenGLES程序
        mProgram = GLES20.glCreateProgram();
        //将顶点着色器加入到OpenGLES程序
        GLES20.glAttachShader(mProgram, vertexShader);
        //将片元着色器加入到OpenGLES程序
        GLES20.glAttachShader(mProgram, fragmentShader);
        //链接OpenGLES程序
        GLES20.glLinkProgram(mProgram);

        //获取 shader 代码中的变量索引(句柄)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //获取片元着色器的vColor成员的句柄
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
        // 获取 uMVPMatrix 的索引
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
    }

    private int loadShader(int type, String shaderCode) {
        //根据type创建顶点着色器或者片元着色器
        int shader = GLES20.glCreateShader(type);
        //将资源加入到着色器中，并编译
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        // 计算宽高比
        float ratio = (float) width / height;
        // 设置透视投影，`ratio` 表示屏幕的宽高比。创建了一个宽高比为 `ratio`，近平面距离为 3，远平面距离为 7
        // 的透视投影矩阵，可以用于将三维物体投影到屏幕上进行渲染。
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        // 设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 3.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        // 计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    //顶点个数
    @Override
    public void onDrawFrame(GL10 gl) {
        // 使用glClearColor函数所设置的颜色进行清屏
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //使用创建的OpenGLES程序
        GLES20.glUseProgram(mProgram);

        //启用 vertex 句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //绑定 vertex 坐标值
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        //设置绘制三角形的颜色
        GLES20.glEnableVertexAttribArray(mColorHandle);
        // 绑定color的坐标值
        // 用于指定顶点属性数据的格式，告诉 OpenGL ES 顶点数据存储的方式，以便它正确地解析和使用这些数据。
        GLES20.glVertexAttribPointer(mColorHandle, COLOR, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // 在绘制的时候为 uMVPMatrix 赋值
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

}
