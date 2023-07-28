package com.blend.ndkadvanced.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SquareRender implements GLSurfaceView.Renderer {

    private int mProgram;
    private FloatBuffer mVertexBuffer;
    private ShortBuffer mVertexIndexBuffer;

    private final String vertexShaderCode =
            "attribute vec4 vPosition; " +
                    "uniform mat4 uMVPMatrix;" +
                    "void main() {" +
                    "gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    // 片元着色器代码，就是Fragment Shader
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    // 这里定义为16,是因为是一个4 * 4的矩阵
    private final float[] mMVPMatrix = new float[16];

    //设置颜色，依次为红绿蓝和透明通道
    private float color[] = {1.0f, 1.0f, 0f, 1.0f};

    private int mPositionHandle;
    private int mColorHandle;

    private int mMatrixHandle;

    // VERTEX 保存了 4 个顶点的坐标，VERTEX_INDEX 保存了顶点的绘制顺序。
    private static final float[] VERTEX = {   // in counterclockwise order:
            1, 1, 0,   // top right
            -1, 1, 0,  // top left
            -1, -1, 0, // bottom left
            1, -1, 0,  // bottom right
    };

    // 0 -> 1 -> 2 绘制的是 右上 -> 左上 -> 左下 上半个三角形，逆时针方向，
    // 而 0 -> 2 -> 3 则绘制的是 右上 -> 左下 -> 右下 下半个三角形，也是逆时针方向，这两个三角形则“拼接”成了一个矩形。
    // 这个绘制顺序,是针对VERTEX数组里面的下标来的
    private static final short[] VERTEX_INDEX = {0, 1, 2, 0, 2, 3};

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置清屏的颜色， 在使用OpenGL时，很多地方采用的参数变化范围都是从0到1
        GLES20.glClearColor(0f, 0.5f, 0f, 1.0f);

        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX);
        mVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX);
        mVertexIndexBuffer.position(0);

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
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        // 获取 uMVPMatrix 的索引
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
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

        // 计算变换矩阵
        // 透视投影,m 是保存变换矩阵的数组，offset 是开始保存的下标偏移量
        // fovy 是 y 轴的 field of view 值，也就是视角大小，视角越大，看到的范围就越广
        // aspect 是 Screen space 的宽高比。zNear 和 zFar 则是视锥体近平面和远平面的 z 轴坐标了。
        Matrix.perspectiveM(mMVPMatrix, 0, 90, (float) width / height, 0.1f, 100f);
        // 由于历史原因，Matrix.perspectiveM 会让 z 轴方向倒置，所以左乘投影矩阵之后，顶点 z 坐标需要在 -zNear~-zFar 范围内才会可见。
        // 前面我们顶点的 z 坐标都是 0，我们可以把它修改为 -0.1f~-100f 之间的值，也可以通过一个位移变换来达到此目的：
        // 沿着 z 轴的反方向移动 2.5，这样就能把 z 坐标移到 -0.1f~-100f 了。
        Matrix.translateM(mMVPMatrix, 0, 0f, 0f, -2.5f);
    }

    //顶点个数
    @Override
    public void onDrawFrame(GL10 gl) {
        // 使用glClearColor函数所设置的颜色进行清屏
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //使用创建的OpenGLES程序
        GLES20.glUseProgram(mProgram);

        //启用 vertex
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //绑定 vertex 坐标值
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);

        //设置绘制四边形的颜色
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // 在绘制的时候为 uMVPMatrix 赋值
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

        // 用 glDrawElements 来绘制，mVertexIndexBuffer 指定了顶点绘制顺序
        // 索引法绘制正方形
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
