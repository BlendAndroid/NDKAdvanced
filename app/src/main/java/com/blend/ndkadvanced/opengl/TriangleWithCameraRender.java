package com.blend.ndkadvanced.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// 写变换的矩阵，然后把矩阵交给OpenGL处理，需要用到相机和投影，生成需要的矩阵
/*
相机
根据现实生活中的经历，对一个场景，随着相机的位置、姿态的不同，拍摄出来的画面也是不相同。将相机对应于OpenGL的世界，决定相机
拍摄的结果（也就是最后屏幕上展示的结果），包括相机位置、相机观察方向以及相机的UP方向。

用于设置一个摄像机的位置和方向，通常用于创建一个视图矩阵，将场景中的物体从世界坐标系转换为相机坐标系

相机位置：相机的位置是比较好理解的，就是相机在3D空间里面的坐标点。
相机观察方向：相机的观察方向，表示的是相机镜头的朝向，你可以朝前拍、朝后拍、也可以朝左朝右，或者其他的方向。
相机UP方向：相机的UP方向，可以理解为相机顶端指向的方向。比如你把相机斜着拿着，拍出来的照片就是斜着的，你倒着拿着，拍出来的就是倒着的。
Matrix.setLookAtM (float[] rm,      //接收相机变换矩阵
                int rmOffset,       //变换矩阵的起始位置（偏移量）
                float eyeX,float eyeY, float eyeZ,   //相机位置,可以理解为人眼看过去的位置,Z越大,图像就越小
                float centerX,float centerY,float centerZ,  //观测点位置
                float upX,float upY,float upZ)  //up向量在xyz上的分量

- `rm`：输出的视图矩阵，即将场景中的物体从世界坐标系转换为相机坐标系的矩阵。
- `rmOffset`：输出的矩阵中的起始偏移量。
- `eyeX`、`eyeY`、`eyeZ`：摄像机的位置坐标。
- `centerX`、`centerY`、`centerZ`：摄像机的目标点坐标，也就是摄像机要对准的点。
- `upX`、`upY`、`upZ`：摄像机向上的方向，通常为 (0, 1, 0)。

其实现原理是构造一个视图矩阵，将场景中的物体从世界坐标系转换为相机坐标系。其中，摄像机的位置和目标点坐标决定了相机的朝向，向量 `up` 则确定了相机的倾斜方向。
 */

/*
投影
用相机看到的3D世界，最后还需要呈现到一个2D平面上，这就是投影了。Android OpenGLES的世界中，投影有两种，一种是正交投影，另外一种是透视投影。

使用正交投影，物体呈现出来的大小不会随着其距离视点的远近而发生变化。
用于创建一个正交投影矩阵。正交投影矩阵可以将三维坐标系中的物体投影到二维平面上，通常用于创建 2D 游戏或者界面

Matrix.orthoM (float[] m,           //接收正交投影的变换矩阵
                int mOffset,        //变换矩阵的起始位置（偏移量）
                float left,         //相对观察点近面的左边距
                float right,        //相对观察点近面的右边距
                float bottom,       //相对观察点近面的下边距
                float top,          //相对观察点近面的上边距
                float near,         //相对观察点近面距离
                float far)          //相对观察点远面距离

- `m`：输出的正交投影矩阵。
- `mOffset`：输出矩阵中的起始偏移量。
- `left`、`right`、`bottom`、`top`：定义了一个正交投影空间的六个面，分别为左、右、下、上、近、远面。这些参数定义了投影矩阵的宽度、高度和深度。
- `near`、`far`：定义了近面和远平面的距离。

其实现原理是将正交投影空间映射到一个裁剪空间中。投影变换将位于投影平面之间的物体裁剪到投影平面之外，从而实现了投影效果。正交投影是一个比
较简单的投影方式，它保持了物体在投影平面上的大小不变，但是在远离投影平面的时候可能会出现物体变小的情况。

使用透视投影，物体离视点越远，呈现出来的越小。离视点越近，呈现出来的越大。

透视投影矩阵可以将三维坐标系中的物体投影到二维平面上，通常用于创建 3D 游戏或者界面。

Matrix.frustumM (float[] m,         //接收透视投影的变换矩阵
                int mOffset,        //变换矩阵的起始位置（偏移量）
                float left,         //相对观察点近面的左边距
                float right,        //相对观察点近面的右边距
                float bottom,       //相对观察点近面的下边距
                float top,          //相对观察点近面的上边距
                float near,         //相对观察点近面距离
                float far)          //相对观察点远面距离

- `m`：输出的透视投影矩阵。
- `offset`：输出矩阵中的起始偏移量。
- `left`、`right`、`bottom`、`top`：定义了一个透视投影空间的六个面，分别为左、右、下、上、近、远面。这些参数定义了投影矩阵的宽度、高度和深度。
- `near`、`far`：定义了近面和远平面的距离。

透视投影会对物体的大小进行修正，使得距离观察者越远的物体看起来越小，距离观察者越近的物体看起来越大。透视投影的效果更为逼真，更适合 3D 场景的渲染。

使用变换矩阵
实际上相机设置和投影设置并不是真正的设置，而是通过设置参数，得到一个使用相机后顶点坐标的变换矩阵，和投影下的顶点坐标变换矩阵，我们还需要把矩阵传入给
顶点着色器，在顶点着色器中用传入的矩阵乘以坐标的向量，得到实际展示的坐标向量。注意，是矩阵乘以坐标向量，不是坐标向量乘以矩阵，矩阵乘法是不满足交换律的。
而通过上面的相机设置和投影设置，我们得到的是两个矩阵，为了方便，我们需要将相机矩阵和投影矩阵相乘，得到一个实际的变换矩阵，再传给顶点着色器。矩阵相乘：

Matrix.multiplyMM (float[] result, //接收相乘结果
                int resultOffset,  //接收矩阵的起始位置（偏移量）
                float[] lhs,       //左矩阵
                int lhsOffset,     //左矩阵的起始位置（偏移量）
                float[] rhs,       //右矩阵
                int rhsOffset)     //右矩阵的起始位置（偏移量）
 */
public class TriangleWithCameraRender implements GLSurfaceView.Renderer {

    private int mProgram;
    private FloatBuffer vertexBuffer;

    private final String vertexShaderCode =
            "attribute vec4 vPosition; " +
                    "uniform mat4 vMatrix;" +
                    "void main() {" +
                    "gl_Position = vMatrix * vPosition;" +
                    "}";

    // 片元着色器代码，就是Fragment Shader
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
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

    //顶点个数
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    //顶点之间的偏移量
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节

    //设置颜色，依次为红绿蓝和透明通道
    private float color[] = {1.0f, 0f, 0f, 1.0f};


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
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // 在绘制的时候为 uMVPMatrix 赋值
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
