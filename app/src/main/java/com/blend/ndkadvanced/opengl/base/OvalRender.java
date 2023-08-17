package com.blend.ndkadvanced.opengl.base;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// GLES20.glDrawArrays的第一个参数表示绘制方式，第二个参数表示偏移量，第三个参数表示顶点个数。
/*
int GL_POINTS       //将传入的顶点坐标作为单独的点绘制
int GL_LINES        //将传入的坐标作为单独线条绘制，ABCDEFG六个顶点，绘制AB、CD、EF三条线
int GL_LINE_STRIP   //将传入的顶点作为折线绘制，ABCD四个顶点，绘制AB、BC、CD三条线
int GL_LINE_LOOP    //将传入的顶点作为闭合折线绘制，ABCD四个顶点，绘制AB、BC、CD、DA四条线。
int GL_TRIANGLES    //将传入的顶点作为单独的三角形绘制，ABCDEF绘制ABC,DEF两个三角形
int GL_TRIANGLE_FAN    //将传入的顶点作为扇面绘制，ABCDEF绘制ABC、ACD、ADE、AEF四个三角形
int GL_TRIANGLE_STRIP   //将传入的顶点作为三角条带绘制，ABCDEF绘制ABC,BCD,CDE,DEF四个三角形

绘制小结
GL_TRIANGLE_STRIP
由上面的注释，我们可以知道，GL_TRIANGLE_STRIP的方式绘制连续的三角形，比直接用GL_TRIANGLES的方式绘制三角形少好多个顶点，效率会高很多。另外，GL_TRIANGLE_STRIP并不是只能绘制连续的三角形构成的物体，我们只需要将不需要重复绘制的点重复两次即可。比如，传入ABCDEEFFGH坐标，就会得到ABC、BCD、CDE以及FGH四个三角形

GL_TRIANGLE_FAN
扇面绘制是以第一个为零点进行绘制，通常我们绘制圆形，圆锥的锥面都会使用到，值得注意的是，最后一个点的左边应当与第二个点重合，在计算的时候，起点角度为0度，终点角度应包含360度。

顶点法和索引法
上述提到的绘制，使用的都是GLES20.glDrawArrays，也就是顶点法，是根据传入的定点顺序进行绘制的。还有一个方法进行绘制GLES20.glDrawElements，称之为索引法，是根据索引序列，在顶点序列中找到对应的顶点，并根据绘制的方式，组成相应的图元进行绘制。
顶点法拥有的绘制方式，索引法也都有。相对于顶点法在复杂图形的绘制中无法避免大量顶点重复的情况，索引法可以相对顶点法减少很多重复顶点占用的空间。

`GL_TRIANGLE_FAN` 是OpenGL中的一种图元类型，用于绘制三角形扇形。它的绘制方式是从一个顶点开始，依次连接后续的顶点，形成一系列三角形，直到最后一个顶点与第一个顶点相连为止。

具体来说，`GL_TRIANGLE_FAN` 绘制时需要指定一个顶点数组，顶点数组中的第一个顶点就是扇形的中心点，后面的顶点就按照顺序构成扇形的边界。OpenGL将按照这个规则依次连接所有的顶点，形成一系列三角形。

例如，假设我们有一个包含6个顶点的顶点数组：

float vertices[] = {
    0.0f, 0.0f, 0.0f,
    0.5f, 0.0f, 0.0f,
    0.5f, 0.5f, 0.0f,
    0.0f, 0.5f, 0.0f,
    -0.5f, 0.5f, 0.0f,
    -0.5f, 0.0f, 0.0f
};

我们可以使用 `glDrawArrays` 函数以 `GL_TRIANGLE_FAN` 的方式绘制一个三角形扇形，绘制方式如下：

glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

这将从顶点数组的第一个元素开始，将顶点数组中的所有顶点连接起来，形成一个三角形扇形。具体来说，它将连接以下一系列顶点：

顶点0 -> 顶点1 -> 顶点2 -> 顶点3 -> 顶点4 -> 顶点5 -> 顶点1

这将形成一个由6个三角形组成的扇形，如下所示：

```
  2----3
 /|    |\
1 |    | 4
 \|    |/
  0----5
```

其中的数字代表顶点的下标。注意，这个扇形的中心点是顶点数组中的第一个顶点（0）。
 */
public class OvalRender implements GLSurfaceView.Renderer {

    private int mProgram;

    private FloatBuffer vertexBuffer;

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "uniform mat4 vMatrix;" +
                    "void main() {" +
                    "  gl_Position = vMatrix * vPosition;" +
                    "}";

    // 片元着色器代码，就是Fragment Shader
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";


    // 每个顶点的个数
    static final int COORDS_PER_VERTEX = 3;

    //顶点之间的偏移量
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 每个顶点四个字节

    //设置颜色，依次为红绿蓝和透明通道
    private float color[] = {1.0f, 0f, 0f, 1.0f};

    private float[] mProjectMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];

    private int mPositionHandle;
    private int mColorHandle;
    private int mMatrixHandle;

    private float radius = 1.0f;
    private int n = 360;  //切割份数

    private float[] shapePos;

    private float height = 0.0f;

    public void setMatrix(float[] matrix) {
        this.mMVPMatrix = matrix;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置清屏的颜色， 在使用OpenGL时，很多地方采用的参数变化范围都是从0到1
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        shapePos = createPositions();

        //申请底层空间，先初始化buffer，数组的长度*4，因为一个float占4个字节
        ByteBuffer bb = ByteBuffer.allocateDirect(
                shapePos.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(shapePos);
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
        // 获取 uMVPMatrix 的索引
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix");
    }

    private float[] createPositions() {
        ArrayList<Float> data = new ArrayList<>();
        data.add(0.0f);             //设置圆心坐标
        data.add(0.0f);
        data.add(height);
        float angDegSpan = 360f / n;
        // 这里增加3,是因为每个顶点,由xyz组成
        for (float i = 0; i < 360 + angDegSpan; i += angDegSpan) {
            data.add((float) (radius * Math.sin(i * Math.PI / 180f)));
            data.add((float) (radius * Math.cos(i * Math.PI / 180f)));
            data.add(height);
        }
        float[] f = new float[data.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = data.get(i);
        }
        return f;
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
        //计算宽高比
        float ratio = (float) width / height;
        //设置透视投影
        Matrix.frustumM(mProjectMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    //顶点个数
    @Override
    public void onDrawFrame(GL10 gl) {
        // 使用glClearColor函数所设置的颜色进行清屏
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

        // 最后的count为/3的
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, shapePos.length / 3);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

}
