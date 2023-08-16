package com.blend.ndkadvanced.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 启用纹理映射后，如果想把一幅纹理映射到相应的几何图元，就必须告诉GPU如何进行纹理映射，也就是为图元的顶点指定恰当的纹理坐标。
 * 2D纹理坐标在x和y轴上，范围为0到1之间。使用纹理坐标获取纹理颜色叫做采样(Sampling)。纹理坐标起始于(0, 0)，也就是纹理图片
 * 的左下角，终始于(1, 1)，即纹理图片的右上角。
 * <p>
 * `sampler2D` 是 GLSL 中的一种变量类型，用于表示 2D 纹理对象。它可以在片段着色器中使用，将纹理图像映射到几何形状的表面上，
 * 实现纹理贴图的效果。
 * `sampler2D` 通常与纹理坐标一起使用，纹理坐标指定图像中的一个特定点，而 `sampler2D` 则表示该点的颜色值。在片段着色器中，
 * 可以使用 `texture2D` 函数来获取 `sampler2D` 中的颜色值。
 * <p>
 * public static void glGenTextures(int n, int[] textures, int offset);
 * `n` 参数指定要生成的纹理对象数量，
 * `textures` 参数是一个整型数组，用于存储生成的纹理对象的名称，
 * `offset` 参数表示从数组的哪个位置开始存储。
 * 将纹理坐标映射到顶点坐标
 */
public class PictureRender implements GLSurfaceView.Renderer {

    private int mProgram;
    private FloatBuffer mTexVertexBuffer;
    private FloatBuffer mVertexBuffer;
    private ShortBuffer mVertexIndexBuffer;

    // a_texCoord 是纹理坐标，它是一个二维向量，由 u 和 v 两个分量组成，分别表示横向和纵向的纹理坐标。
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 a_texCoord;" +
                    "varying vec2 v_texCoord;" +
                    "void main() {" +
                    " gl_Position = uMVPMatrix * vPosition;" +
                    " v_texCoord = a_texCoord;" +
                    "}";

    // 片元着色器代码，就是Fragment Shader
    // 通过 varying 关键字，将 v_texCoord 变量从顶点着色器传递到片元着色器
    // sampler2D，是GLSL的变量类型之一的取样器。texture2D也有提到，它是GLSL的内置函数，
    // 用于2D纹理取样，根据纹理取样器和纹理坐标，可以得到当前纹理取样得到的像素颜色。
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec2 v_texCoord;" +
                    "uniform sampler2D s_texture;" +
                    "void main() {" +
                    " gl_FragColor = texture2D(s_texture, v_texCoord);" +
                    "}";

    // 这里定义为16,是因为是一个4 * 4的矩阵
    private final float[] mMVPMatrix = new float[16];

    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mTexSamplerHandle;

    private int mMatrixHandle;

    // VERTEX 保存了 4 个顶点的坐标，VERTEX_INDEX 保存了顶点的绘制顺序。
    // 设置顶点坐标
    private static final float[] VERTEX = {   // in counterclockwise order:
            1, 1, 0,   // top right
            -1, 1, 0,  // top left
            -1, -1, 0, // bottom left
            1, -1, 0,  // bottom right
    };

    // 设置纹理坐标，纹理坐标的范围是 0~1，左下角是 (0, 0)，右上角是 (1, 1)。
    private static final float[] TEX_VERTEX = {
            1, 0,  // bottom right
            0, 0,  // bottom left
            0, 1,  // top left
            1, 1,  // top right
    };

    // 0 -> 1 -> 2 绘制的是 右上 -> 左上 -> 左下 上半个三角形，逆时针方向，
    // 而 0 -> 2 -> 3 则绘制的是 右上 -> 左下 -> 右下 下半个三角形，也是逆时针方向，这两个三角形则“拼接”成了一个矩形。
    private static final short[] VERTEX_INDEX = {0, 1, 2, 0, 2, 3};

    private final Context mContext;
    private int mTexName;

    public PictureRender(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 设置清屏的颜色， 在使用OpenGL时，很多地方采用的参数变化范围都是从0到1
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX);
        mVertexBuffer.position(0);


        mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX);
        mTexVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX);
        mVertexIndexBuffer.position(0);

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

        // 完成上面的步骤,创建,加载和链接OpenGLES程序

        //获取 shader 代码中的变量索引(句柄)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");

        // 启动句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);

        // 传入顶点坐标
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, mVertexBuffer);

        // 传入纹理坐标
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                mTexVertexBuffer);


        initTexture();

    }

    private void initTexture() {
        //一个整型数组，用于存储生成的纹理对象的名称
        int[] texNames = new int[1];
        // 通过 glGenTextures 创建生成指定数量的纹理对象，纹理对象用于存储和管理纹理数据，以便将纹理映射到几何形状的表面上
        GLES20.glGenTextures(1, texNames, 0);
        // 纹理句柄
        mTexName = texNames[0];
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(mContext.getResources().getAssets().open("picture.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 通过 glActiveTexture 激活指定编号的纹理,一共有32个
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 通过 glBindTexture 将新建的纹理和编号绑定起来, 绑定纹理句柄，将对象绑定到环境的纹理单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexName);

        // 设置纹理对象的参数
        // 纹理对象的最小化和最大化过滤参数。`GL_TEXTURE_MIN_FILTER` 和 `GL_TEXTURE_MAG_FILTER` 分别表示最小化
        // 和最大化过滤参数。`GL_LINEAR` 表示使用线性过滤器来处理纹理。
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        // 纹理对象的环绕参数。`GL_TEXTURE_WRAP_S` 和 `GL_TEXTURE_WRAP_T` 分别表示纹理的 水平方向 和 垂直方向 轴的环绕参数。
        // 指定了在纹理坐标超出范围时，如何对纹理进行处理。`GL_REPEAT`：在超出纹理坐标范围时，纹理会重复出现。
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        // texImage2D 将纹理图像加载到纹理对象中
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,    // 纹理类型
                0,  // 纹理的层次，0表示基本图像层，可以理解为直接贴图
                bitmap, // 纹理图像
                0); // 纹理边框尺寸，必须为 0 或 1
        bitmap.recycle();
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

        // 在绘制的时候为 uMVPMatrix 赋值
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);

        // 设置纹理值
        GLES20.glUniform1i(mTexSamplerHandle, 0);

        // 用 glDrawElements 来绘制，mVertexIndexBuffer 指定了顶点绘制顺序
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
