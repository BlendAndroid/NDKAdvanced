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

public class PictureRender implements GLSurfaceView.Renderer {

    private int mProgram;
    private FloatBuffer mTexVertexBuffer;
    private FloatBuffer mVertexBuffer;
    private ShortBuffer mVertexIndexBuffer;

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
    private static final float[] VERTEX = {   // in counterclockwise order:
            1, 1, 0,   // top right
            -1, 1, 0,  // top left
            -1, -1, 0, // bottom left
            1, -1, 0,  // bottom right
    };

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
        GLES20.glClearColor(0f, 0.5f, 0f, 1.0f);

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

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, mVertexBuffer);

        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                mTexVertexBuffer);


        initTexture();

    }

    private void initTexture() {
        //生成纹理id
        int[] texNames = new int[1];
        // 通过 glGenTextures 创建纹理对象
        GLES20.glGenTextures(1, texNames, 0);
        mTexName = texNames[0];
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(mContext.getResources().getAssets().open("picture.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 通过 glActiveTexture 激活指定编号的纹理,一共有32个
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 通过 glBindTexture 将新建的纹理和编号绑定起来, 绑定纹理id，将对象绑定到环境的纹理单元
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexName);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        // texImage2D 把图片数据拷贝到纹理中
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,    // 纹理类型
                0,  // 纹理的层次，0表示基本图像层，可以理解为直接贴图
                bitmap, // 纹理图像
                0); // 纹理边框尺寸，可以理解为图片的大小
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
        GLES20.glUniform1i(mTexSamplerHandle, 0);

        // 用 glDrawElements 来绘制，mVertexIndexBuffer 指定了顶点绘制顺序
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
