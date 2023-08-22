package com.blend.ndkadvanced.camera1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ARGB，一个像素占用4个字节
 * <p>
 * YUV码流的存储格式其实与其采样的方式密切相关，主流的采样方式有三种，YUV4:4:4，YUV4:2:2，YUV4:2:0，并没有什么优缺点，摆放方式和
 * 硬件有关。
 * <p>
 * Android Camera对象通过setPreviewCallback 函数，在onPreviewFrame(byte[] data,Camera camera)中回调采集
 * 的数据就是NV21格式。而H264编码的输入数据却为I420格式。因此，当我们采集到摄像头数据之后需要将NV21转为I420。
 * NV21和I420都是属于YUV420格式。而NV21是一种two-plane模式，即Y和UV分为两个Plane(平面)，但是UV（CbCr）交错存储2个平面，
 * 而不是分为三个。这种排列方式被称之为YUV420SP(Semi-Planar)，而I420则称之为YUV420P(Planar)。(Y:明亮度、灰度，UV:色度、饱和度)
 * <p>
 * RGB24使用24位来表示一个像素，RGB分量都用8位表示，取值范围为0-255。在一个2*2的像素区域，RRG暂用的字节数为2*2*3=12字节。
 * 那么用yuv表示，占用的字节数为4(Y)+1(u)+1(v)=6字节,其中Y占用4个字节，U和V各占用1字节，比例为4:1:1
 * 所以在一个宽高为w*h的设备上，使用rgb表示编码占用的字节数为w*h*3,使用yuv表示暂用的内存为w*h*+w*h/4+w*h/4 = w*h*3/2.
 * <p>
 * 首先需要了解的是yuv有很多编码格式，其中yuv420就是一种，而nv21又是yuv420的一种。并且nv21是针对android设备的视频编码。
 * nv21编码格式:比如一张1920*1280的图片，经过nv21编码后，会变成前面1920*1280字节全是Y，从1920*1280字节长度开始，
 * U和V会交替排列，它们的字节长度分别为1920*1280/4。
 * I420也是YUV420编码格式的一种，由于android手机厂商的原因，摄像头采集到的数据永远都是经过NV21编码的数据，但是对于这种
 * 数据不能够显示在苹果或windows平台，那么需要对这个编码格式的数据需要重新编码，其中I420这种编码格式，所有的厂商都是适配的。
 * <p>
 * I420编码格式:比如一张1920*1280的图片，经过I420编码后，会变成前面1920*1280字节全是Y，从1920*1280字节长度开始，会先排列U，
 * 总字节长度为1920*1280/4，从1920*1280+1920*1280/4开始排列V，字节长度为1920*1280/4，所以总的字节长度适合NV21一样的，只是UV的编码顺序不一样
 */
public class Camera1SurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "Camera1SurfaceView";

    private Camera.Size size;
    private Camera mCamera;
    private ImageView mImageView;

    public Camera1SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 监听SurfaceView打开操作
        getHolder().addCallback(this);
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // 打开后开始预览
        startPreview();
    }

    // 摄像头输出缓冲区
    private byte[] buffer;
    // 设置输出buffer,如果出现对角线不清楚的情况,一般是宽高没有设置对
    private byte[] outputBuffer;

    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        Camera.Parameters parameters = mCamera.getParameters();

        // 获取到预览尺寸
        size = parameters.getPreviewSize();
        try {
            // 绑定SurfaceView进行输出
            mCamera.setPreviewDisplay(getHolder());
            // 旋转90度, 设置预览的方向
            mCamera.setDisplayOrientation(90);

            Log.e(TAG, "startPreview size.width: " + size.width);
            Log.e(TAG, "startPreview size.height: " + size.height);

            // 设置缓冲区的大小，无论是哪种排列方式，YUV420的数据量都为: width × height * 3 / 2
            buffer = new byte[size.width * size.height * 3 / 2];
            // 设置输出的buffer的长度
            outputBuffer = new byte[buffer.length];
            // 缓冲区与Camera绑定，不断的从buffer获取数据
            mCamera.addCallbackBuffer(buffer);
            // 设置渲染回调，回调onPreviewFrame方法
            mCamera.setPreviewCallbackWithBuffer(this);
            // 开始渲染
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }


    private volatile boolean isCapture;

    public void startCapture(ImageView imgCamera1Bitmap) {
        mImageView = imgCamera1Bitmap;
        isCapture = true;
    }

    // 接收到摄像头的回调数据
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (isCapture) {
            isCapture = false;
            portraitData2Raw(bytes);
            capture(outputBuffer);
        }
        // 不断的回调数据
        mCamera.addCallbackBuffer(bytes);
    }

    // 开始旋转数据，不旋转的话，就是横屏的，顺时针旋转
    // 将竖屏的数据，旋转成横屏的数据
    private void portraitData2Raw(byte[] data) {
        int width = size.width;
        int height = size.height;

        // Y的长度
        int y_len = width * height;

        // UV的高度为y数据高的一半
        int uvHeight = height / 2;

        int k = 0;

        // 先遍历Y
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                outputBuffer[k++] = data[width * i + j];
            }
        }

        // 再遍历UV，NV21是交替出现的
        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                // 先取V
                outputBuffer[k++] = data[y_len + width * i + j];
                // 再取U
                outputBuffer[k++] = data[y_len + width * i + j + 1];
            }
        }
    }


    private void capture(byte[] temp) {
        //保存一张照片
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";  //jpeg文件名定义
        File sdRoot = getContext().getExternalCacheDir();

        File pictureFile = new File(sdRoot, fileName);
        if (!pictureFile.exists()) {
            try {
                pictureFile.createNewFile();

                // 将NV21数据保存成图片
                FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                // ImageFormat.NV21 and ImageFormat.YUY2 for now，I420是不支持的
                YuvImage image = new YuvImage(temp, ImageFormat.NV21, size.height, size.width, null);   //将NV21 data保存成YuvImage
                //图像压缩，将NV21格式图片，以质量100压缩成Jpeg，并得到JPEG数据流
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, fileOutputStream);

                // 保存成Bitmap
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.height, size.width), 100, stream);
                Bitmap newBitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                stream.close();

                mImageView.setImageBitmap(newBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
