package com.blend.ndkadvanced.camerax;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import com.blend.ndkadvanced.utils.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static com.blend.ndkadvanced.utils.FileUtils.writeBytes;
import static com.blend.ndkadvanced.utils.FileUtils.writeContent;


public class CameraXHelper implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {
    private static final String TAG = "CameraXHelper";
    int width = 480;
    int height = 640;
    private final HandlerThread handlerThread;
    //    直播中  480 640
    private final CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private final TextureView textureView;

    private volatile boolean isCapture;
    private ImageView mImageView;
    private final Activity mActivity;

    public CameraXHelper(LifecycleOwner lifecycleOwner, TextureView textureView) {
        mActivity = (Activity) lifecycleOwner;
        this.textureView = textureView;
        //子线程中回调
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        CameraX.bindToLifecycle(lifecycleOwner, getPreView(), getAnalysis());
    }

    private Preview getPreView() {
        PreviewConfig previewConfig = new PreviewConfig.Builder().setTargetResolution(new Size(width, height)).setLensFacing(currentFacing).build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        return preview;
    }

    private ImageAnalysis getAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(width, height))
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);
        return imageAnalysis;
    }

    //摄像
    @Override
    public void onUpdated(Preview.PreviewOutput output) {

        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView.getSurfaceTexture() != surfaceTexture) {
            if (textureView.isAvailable()) {
                // 当切换摄像头时，会报错
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);
                parent.requestLayout();
            }
            textureView.setSurfaceTexture(surfaceTexture);
        }

    }

    private final ReentrantLock lock = new ReentrantLock();
    private byte[] y;
    private byte[] u;
    private byte[] v;
    private MediaCodec mediaCodec;
    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        Log.i(TAG, "analyze: " + image.getWidth() + "  height " + image.getHeight());
        lock.lock();
//          能够 播放  H264码流   x264     摸索   x264
//3 ge
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        // 重复使用同一批byte数组，减少gc频率
        if (y == null) {
//            初始化y v  u
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
        }

        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
            int stride = planes[0].getRowStride();
            Size size = new Size(image.getWidth(), image.getHeight());
            int width = size.getHeight();
            int height = image.getWidth();
            Log.i(TAG, "analyze: " + width + "  height " + height);
            if (nv21 == null) {
                nv21 = new byte[height * width * 3 / 2];
                nv21_rotated = new byte[height * width * 3 / 2];
            }

            if (mediaCodec == null) {
                initCodec(size);
            }
            ImageUtil.yuvToNv21(y, u, v, nv21, height, width);
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, height, width);
            if (isCapture) {
                isCapture = false;
                //TODO 这里是NV21的数据格式
                capture(nv21_rotated, width, height);
            }
            byte[] temp = ImageUtil.nv21toNV12(nv21_rotated, nv12);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int inIndex = mediaCodec.dequeueInputBuffer(100000);
            if (inIndex >= 0) {
                ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
                byteBuffer.clear();
                byteBuffer.put(temp, 0, temp.length);
                mediaCodec.queueInputBuffer(inIndex, 0, temp.length,
                        0, 0);
            }
            int outIndex = mediaCodec.dequeueOutputBuffer(info, 100000);
            if (outIndex >= 0) {
                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
                byte[] ba = new byte[byteBuffer.remaining()];
                byteBuffer.get(ba);
                Log.e(TAG, "ba = " + ba.length + "");
                writeContent(ba);
                writeBytes(ba);
                mediaCodec.releaseOutputBuffer(outIndex, false);
            }
        }

        lock.unlock();

    }

    private void initCodec(Size size) {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

            final MediaFormat format = MediaFormat.createVideoFormat("video/avc",
                    size.getHeight(), size.getWidth());
            //设置帧率
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 8000_000);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);//2s一个I帧
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void captureImage(ImageView imageView) {
        mImageView = imageView;
        isCapture = true;
    }

    private void capture(byte[] temp, int width, int height) {
        //保存一张照片
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";  //jpeg文件名定义
        File sdRoot = mActivity.getExternalCacheDir();

        File pictureFile = new File(sdRoot, fileName);
        if (!pictureFile.exists()) {
            try {
                pictureFile.createNewFile();

                FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                //ImageFormat.NV21 and ImageFormat.YUY2 for now，I420是不支持的
                YuvImage image = new YuvImage(temp, ImageFormat.NV21, width, height, null);   //将NV21 data保存成YuvImage
                //图像压缩，将NV21格式图片，以质量100压缩成Jpeg，并得到JPEG数据流
                image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, fileOutputStream);

                // 保存成Bitmap
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
                Bitmap newBitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                stream.close();

                mActivity.runOnUiThread(() -> mImageView.setImageBitmap(newBitmap));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}