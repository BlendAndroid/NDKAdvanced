package com.blend.ndkadvanced.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/*
 * 光栅化是把点、线、三角形映射到屏幕上的像素点的过程（每个映射区域叫一个 Fragment），也就是生成 Fragment
 * 的过程。通常一个 Fragment 对应于屏幕上的一个像素，但高分辨率的屏幕可能会用多个像素点映射到一个 Fragment，
 * 以减少 GPU 的工作。
 * Shader 用来描述如何绘制（渲染），GLSL 是 OpenGL 的编程语言，全称 OpenGL Shader Language，它的语法类
 * 似于 C 语言。OpenGL 渲染需要两种 Shader：Vertex Shader 和 Fragment Shader。
 * <p>
 * 每个 Vertex 都会执行一遍 Vertex Shader，以确定 Vertex 的最终位置，其 main 函数中必须设置 gl_Position
 * 全局变量，它将作为该 Vertex 的最终位置，进而把 Vertex 组合（assemble）成点、线、三角形。光栅化之后，每个
 * Fragment 都会执行一次 Fragment Shader，以确定每个 Fragment 的颜色，其 main 函数中必须设置 gl_FragColor
 * 全局变量，它将作为该 Fragment 的最终颜色。
 */

/*
 * uniform变量
 * uniform变量是外部application程序传递给（vertex和fragment）shader的变量。因此它是application通过函数glUniform**（）
 * 函数赋值的。在（vertex和fragment）shader程序内部，uniform变量就像是C语言里面的常量（const ），它不能被shader程序修改。
 * （shader只能用，不能改）
 * 如果uniform变量在vertex和fragment两者之间声明方式完全一样，则它可以在vertex和fragment共享使用。（相当于一个被vertex和
 * fragment shader共享的全局变量）
 * uniform变量一般用来表示：变换矩阵，材质，光照参数和颜色等信息。
 * <p>
 * <p>
 * attribute变量
 * attribute变量是只能在vertex shader中使用的变量。（它不能在fragment shader中声明attribute变量，也不能被fragment shader中使用）
 * 一般用attribute变量来表示一些顶点的数据，如：顶点坐标，法线，纹理坐标，顶点颜色等。
 * 在application中，一般用函数glBindAttribLocation（）来绑定每个attribute变量的位置，然后用函数glVertexAttribPointer（）为每
 * 个attribute变量赋值。
 * <p>
 * <p>
 * varying变量
 * varying变量是vertex和fragment shader之间做数据传递用的。一般vertex shader修改varying变量的值，然后fragment shader使用
 * 该varying变量的值。因此varying变量在vertex和fragment shader二者之间的声明必须是一致的。application不能使用此变量。
 */

/*
 * precision mediump float
 * GLSL 中的一个声明，用于指定浮点数精度。在片元着色器中，通常需要对颜色进行处理，因此需要使用浮点数。
 * 然而，使用浮点数会导致精度损失，特别是在移动设备等低性能设备上。因此，为了平衡性能和精度，GLSL 支持不同的浮点数精度等级。
 * `mediump` 表示中等精度，通常适用于大多数情况。如果需要更高的精度，可以使用 `highp`，但这会降低性能。如果需要更低的精度，可以使用
 * `lowp`，但这会降低精度。在声明了浮点数精度之后，可以声明浮点数变量来处理纹理颜色、位置等数据。
 */

/*
 * vec4
 * 是 GLSL 中的一种数据类型，表示一个四维向量，通常用于表示颜色、空间坐标、矩阵等数据。
 * `vec4` 由 4 个浮点数组成，分别表示向量的 x、y、z、w 分量，具体用法和具体含义取决于上下文。例如，表示颜色的 `vec4` 可以使用
 * RGBA 表示，分别表示红、绿、蓝、透明度分量；表示位置的 `vec4` 可以使用 XYZW 表示，分别表示 x、y、z 坐标和 w 分量（通常设置为 1.0）。
 *
 * mat4
 * 是 GLSL 中的一种数据类型，表示一个 4x4 矩阵，通常用于在顶点着色器中进行矩阵变换，例如模型变换、视图变换和投
 * 影变换等。
 * `mat4` 由 16 个浮点数组成，按行优先顺序存储，表示矩阵的每一个元素，具体用法和具体含义取决于上下文
 *
 * attribute一般用于每个顶点都各不相同的量。
 * uniform一般用于对同一组顶点组成的3D物体中各个顶点都相同的量。
 * varying一般用于从顶点着色器传入到片元着色器的量。
 */
public class TriangleRender implements GLSurfaceView.Renderer {

    private int mProgram;
    private FloatBuffer vertexBuffer;

    //顶点坐标数据的初始化，Vertex 序列围成了一个图形
    // OpenGL 坐标系和安卓手机坐标系不是线性对应的，因为手机的宽高比几乎都不是 1。因此我们绘制的形状是变形的，
    // 怎么解决这个问题呢？答案是投影变换（projection）。前面我们已经知道，投影变换用于把 View space 的坐标转换
    // 为 Clip space 的坐标，在这个转换过程中，它还能顺带处理宽高比的问题。
    // 使用较多的是正投影和透视投影，这里我们使用透视投影：Matrix.perspectiveM。通常坐标系的变换都是对顶点坐标进
    // 行矩阵左乘运算，因此我们需要修改我们的 vertex shader 代码：增加uMVPMatrix
    private final String vertexShaderCode =
            "attribute vec4 vPosition; " +
                    "void main() {" +
                    "gl_Position =  vPosition;" +
                    "}";

    // 片元着色器代码，就是Fragment Shader
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    // 顶点坐标数据的初始化，因为是3D模型，每个点坐标是在三维坐标系中，因此，每个点需要3个数来表示
    // OpenGL并不是对堆里面的数据进行操作，而是在直接内存中（Direct Memory），即操作的数据需要
    // 保存到NIO里面的Buffer对象中。而我们上面声明的float[]对象保存在堆中，因此，需要我们将
    // float[]对象转为java.nio.Buffer对象。
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

    // Java 代码需要获取 shader 代码中定义的变量索引，用于在后面的绘制代码中进行赋值，变量索引在 GLSL 程序的生命周期
    // 内（链接之后和销毁之前），都是固定的，只需要获取一次。
    private int mPositionHandle;
    private int mColorHandle;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置清屏的颜色， 在使用OpenGL时，很多地方采用的参数变化范围都是从0到1
        GLES20.glClearColor(0f, 0.5f, 0f, 1.0f);

        //申请底层空间，先初始化buffer，数组的长度*4，因为一个float占4个字节
        ByteBuffer bb = ByteBuffer.allocateDirect(triangleCoords.length * 4);
        // 以本机字节顺序来修改此缓冲区的字节顺序
        // OpenGL在底层的实现是C语言，与Java默认的数据存储字节顺序可能不同，即大端小端问题。因此，为了
        // 保险起见，在将数据传递给OpenGL之前，我们需要指明使用本机的存储顺序。
        bb.order(ByteOrder.nativeOrder());
        //将坐标数据转换为FloatBuffer，用以传入OpenGL ES程序
        vertexBuffer = bb.asFloatBuffer();
        //将给定float[]数据从当前位置开始，依次写入此缓冲区
        vertexBuffer.put(triangleCoords);
        //设置此缓冲区的位置。如果标记已定义并且大于新的位置，则要丢弃该标记。
        vertexBuffer.position(0);

        // 完成上面的步骤，顺利地将float[]转为了FloatBuffer，后面绘制三角形的时候，直接通过成员变量vertexBuffer即可。

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
        // 设置窗口视图
        gl.glViewport(0, 0, width, height);
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


        //绘制三角形
        // 执行完毕之后，GPU 就在显存中处理好帧数据了，但此时并没有更新到 surface 上，是 GLSurfaceView 会在调用
        // renderer.onDrawFrame 之后，调用 eglSwapBuffers，来把显存的帧数据更新到 surface 上的
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
