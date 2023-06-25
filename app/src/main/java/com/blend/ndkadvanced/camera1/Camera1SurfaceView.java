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
 * 而不是分为三个。这种排列方式被称之为YUV420SP，而I420则称之为YUV420P。(Y:明亮度、灰度，UV:色度、饱和度)
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
            // 旋转90度
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

    int index = 0;

    private void capture(byte[] temp) {
        //保存一张照片
        String fileName = "IMG_" + index++ + ".jpg";  //jpeg文件名定义
        File sdRoot = getContext().getExternalCacheDir();

        File pictureFile = new File(sdRoot, fileName);
        if (!pictureFile.exists()) {
            try {
                pictureFile.createNewFile();

                FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                //ImageFormat.NV21 and ImageFormat.YUY2 for now，I420是不支持的
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
