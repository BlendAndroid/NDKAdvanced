package com.blend.ndkadvanced.h264;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * MediaCodec是Android提供的一个多媒体编解码器API，可以实现音视频的编解码、格式转换等功能。它是Android多媒体框架中重要
 * 的一个组件，主要用于实现音视频处理中的编码、解码、滤镜等功能。
 * MediaCodec可以通过硬件加速来提高音视频编解码的效率，而且它提供了丰富的参数设置和事件回调机制，可以方便地控制音视频编解
 * 码的过程和结果，同时也提供了一些优化技巧和工具，有助于优化音视频编解码的性能和质量。
 * 我的理解就是MediaCodec封装了底层的dsp芯片的逻辑，MediaCodec可以利用DSP芯片来进行硬件加速，以提高音视频的编解码速度。
 */
public class H264Player implements Runnable {

    private static final String TAG = "H264Player";

    private Context context;

    private String path;

    private MediaCodec mediaCodec;

    private Surface surface;

    // 设置视频编解码器的参数
    private MediaFormat mediaformat;

    // 是否以视频的形式展示
    private boolean isVideo;

    private ImageView mImageView;

    public H264Player(Context context, String path, Surface surface) {

        this.surface = surface;
        this.path = path;
        this.context = context;

        try {
            try {
                // 使用MediaCodec硬件解码H.264/AVC video
                // h265使用的是video/havc
                mediaCodec = MediaCodec.createDecoderByType("video/avc");
            } catch (Exception e) {
                // dsp芯片不支持硬件解码，需要软件解码
            }

            // 视频的参数信息，这里的宽高是哥伦布编码，因为这里是码流，先写死的数据
            mediaformat = MediaFormat.createVideoFormat("video/avc", 368, 384);
            // 设置帧率，1秒中解码出多少镇
            mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play(boolean isVideo) {
        this.isVideo = isVideo;
        if (isVideo) {
            // 设置配置，并且渲染到surface上，并且不进行加密
            mediaCodec.configure(mediaformat, surface, null, 0);
        } else {
            // 数据不渲染到屏幕上,第二个参数为null，以图片的形式展示,一张图片一张图片的展示
            mediaCodec.configure(mediaformat, null, null, 0);
        }

        // 解码器开始工作
        mediaCodec.start();
        // 因为解码是一个耗时操作，所以需要放在一个线程中
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            decodeH264();
        } catch (Exception e) {
            Log.e(TAG, "decodeH264 run: " + e);
        }
    }

    private void decodeH264() {
        byte[] bytes = null;
        try {
            // 演示操作，因为数据量不大，就把视频流全部加载到bytes中
            // 如果是特别大的视频流，就不能这样全部加在了
            bytes = getBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 在调用MediaCodec的start()方法并返回后，获取可用的输入缓冲区
        // 输入缓冲区用于存储原始的音视频数据
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();

        // 开始的索引
        int startIndex = 0;
        // 结束的索引
        int totalSize = bytes.length;

        int i = 0;

        while (true) {
            // 结束标志
            if (totalSize == 0 || startIndex >= totalSize) {
                break;
            }
            // 通过分隔符，寻找I帧，需要跳出sps和pps的长度，否则会黑屏
            int nextFrameStart = findByFrame(bytes, startIndex + 2, totalSize);

            // 10毫秒内判断是否有可用的缓冲区索引
            int inIndex = mediaCodec.dequeueInputBuffer(10000);
            // 如果Index > 0，就是可用的缓冲区索引
            if (inIndex >= 0) {
                // 通过索引，拿到可用的ByteBuffer
                ByteBuffer byteBuffer = inputBuffers[inIndex];
                // 先清空数据
                byteBuffer.clear();
                // 将一个I帧的数据放入byteBuffer中
                byteBuffer.put(bytes, startIndex, nextFrameStart - startIndex);
                // 将一帧数据放入mediaCodec中，通知dsp芯片进行解码
                mediaCodec.queueInputBuffer(inIndex, 0, nextFrameStart - startIndex, 0, 0);
                // 重新赋值startIndex
                startIndex = nextFrameStart;
            } else {
                // 如果dsp芯片比较忙，就先等待
                continue;
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            // 拿到解码后的数据，是通过输出缓冲区，返回他的索引，在10ms内解码
            int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
            if (outIndex >= 0) {

                // 如果展示的是图片，解析到第50帧
                if (!isVideo) {
                    if (i < 50) {
                        i = decodeBitmap(i, info, outIndex);
                    }
                }

                try {
                    // 控制播放速度，等待时间是33ms一帧，但是这里是有问题的，音视频同步是有问题的
                    // 真正播放一帧的时间 = 解码时间 + 渲染时间 + 等待时间
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 解码后的数据直接丢掉，render参数为true，就能在surface中进行渲染
                // isVideo为true,指针渲染到surface,为false,则自己处理
                mediaCodec.releaseOutputBuffer(outIndex, isVideo);
            } else {

            }
        }

    }

    // 解析图片
    private int decodeBitmap(int i, MediaCodec.BufferInfo info, int outIndex) {

        // 根据索引，获取到一张解码后的YUV图片的数据，都是一个数组
        ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);

        // 定义byteBuffer中的开始和结束的位置
        byteBuffer.position(info.offset);
        byteBuffer.limit(info.offset + info.size);

        // 实例化一个新的数组，长度和byteBuffer的长度一致，用于获取dsp芯片的数据
        byte[] ba = new byte[byteBuffer.remaining()];
        // 将byteBuffer的数据给了ba
        byteBuffer.get(ba);

        YuvImage yuvImage = new YuvImage(ba, ImageFormat.NV21, 368, 384, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, 368, 384), 100, baos);
        byte[] jdata = baos.toByteArray();//rgb
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
        if (bmp != null) {
            if (i > 10) {
                try {
                    mImageView.setImageBitmap(bmp);
                    File myCaptureFile = new File(Environment.getExternalStorageDirectory(), "img.png");
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    bos.flush();
                    bos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            i++;
        }

        return i;
    }

    /**
     * 发现I帧的分隔符
     *
     * @param bytes     原始的视频流
     * @param start     开始索引
     * @param totalSize 结束索引
     * @return I帧的分隔符，没有找到返回-1
     */
    private int findByFrame(byte[] bytes, int start, int totalSize) {
        for (int i = start; i < totalSize - 4; i++) {
            // 寻找I帧，0x00000001
            if (bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x00 && bytes[i + 3] == 0x01) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param path 视频的地址
     * @return 视频流byte数据
     * @throws IOException IO异常
     */
    public byte[] getBytes(String path) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf, 0, size)) != -1)
            bos.write(buf, 0, len);
        buf = bos.toByteArray();
        return buf;
    }

    public void setImageView(ImageView h264PlayerImage) {
        mImageView = h264PlayerImage;
    }
}
