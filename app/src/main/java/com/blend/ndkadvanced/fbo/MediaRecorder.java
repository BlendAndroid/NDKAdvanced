package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.blend.ndkadvanced.utils.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaRecorder {

    private static final String TAG = "MediaRecorder";

    private MediaCodec mMediaCodec;
    private int mWidth;
    private int mHeight;
    private String mPath;
    private Surface mSurface;
    private Handler mHandler;
    // 编码封装格式
    private MediaMuxer mMuxer;
    private EGLContext mGlContext;
    private EGLEnv eglEnv;
    private boolean isStart;
    private Context mContext;

    private long mLastTimeStamp;
    private int track;
    private float mSpeed;

    public MediaRecorder(Context context, String path, EGLContext glContext, int width, int height) {
        mContext = context.getApplicationContext();
        mPath = path;
        mWidth = width;
        mHeight = height;
        mGlContext = glContext;
    }

    public void start(float speed) throws IOException {
        mSpeed = speed;
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        //颜色空间 从 surface当中获得
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        //帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        //关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        //创建编码器
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        //配置编码器
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //创建一个surface
        mSurface = mMediaCodec.createInputSurface();

        //编码一个可以播放的视频
        //混合器 (复用器) 将编码的h.264封装为mp4
        mMuxer = new MediaMuxer(mPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        //开启编码
        mMediaCodec.start();

        // 重点：opengl gpu里面的数据画面，肯定要调用opengl函数获取到
        // 创建子线程
        HandlerThread handlerThread = new HandlerThread("codec-gl");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // 创建OpenGL的环境
                eglEnv = new EGLEnv(mContext, mGlContext, mSurface, mWidth, mHeight);
                isStart = true;
            }
        });

    }

    // 开始编码，根据textureId纹理数据，进行编码
    public void fireFrame(final int textureId, final long timestamp) {
        if (!isStart) {
            return;
        }

        //录制用的opengl已经和handler的线程绑定了 ，所以需要在这个线程中使用录制的opengl
        mHandler.post(new Runnable() {
            public void run() {
                eglEnv.draw(textureId, timestamp);
                // 获取对应的数据
                codec(false);
            }
        });

    }

    // 开始编码，拿到H264的数据
    private void codec(boolean endOfStream) {
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
        }

        // 编码
        while (true) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
            // Log.i(TAG, "run: " + index);
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // 如果是结束那直接退出，否则继续循环
                break;
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.e(TAG, "codec: start");
                // 输出格式发生改变  第一次总会调用所以在这里开启混合器
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                track = mMuxer.addTrack(newFormat);
                mMuxer.start();
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //可以忽略
            } else {
                //调整时间戳
                bufferInfo.presentationTimeUs = (long) (bufferInfo.presentationTimeUs / mSpeed);
                //有时候会出现异常 ： timestampUs xxx < lastTimestampUs yyy for Video track
                if (bufferInfo.presentationTimeUs <= mLastTimeStamp) {
                    bufferInfo.presentationTimeUs = (long) (mLastTimeStamp + 1_000_000 / 25 / mSpeed);
                }
                mLastTimeStamp = bufferInfo.presentationTimeUs;

                //正常则 index 获得缓冲区下标,拿到H264的数据
                ByteBuffer encodedData = mMediaCodec.getOutputBuffer(index);
                //如果当前的buffer是配置信息，不管它 不用写出去
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.e(TAG, "codec: write sps and pps");
                    byte[] outData = new byte[bufferInfo.size];
                    encodedData.get(outData);
                    FileUtils.writeBytes(outData);
                    FileUtils.writeContent(outData);
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    //设置从哪里开始读数据(读出来就是编码后的数据)
                    encodedData.position(bufferInfo.offset);
                    //设置能读数据的总长度
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);

                    Log.e(TAG, "codec: write");

                    // 写入H264数据到文件
                    byte[] outData = new byte[bufferInfo.size];
                    encodedData.get(outData);
                    FileUtils.writeBytes(outData);
                    FileUtils.writeContent(outData);

                    //写出为mp4
                    mMuxer.writeSampleData(track, encodedData, bufferInfo);
                }
                // 释放这个缓冲区，后续可以存放新的编码后的数据啦
                mMediaCodec.releaseOutputBuffer(index, false);
                // 如果给了结束信号 signalEndOfInputStream
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }

    private void codecH264(boolean endOfStream) {
        //给个结束信号
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
            return;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
        Log.i(TAG, "run: " + index);
        if (index >= 0) {
            ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
            MediaFormat mediaFormat = mMediaCodec.getOutputFormat(index);
            Log.i(TAG, "mediaFormat: " + mediaFormat.toString());
            byte[] outData = new byte[bufferInfo.size];
            buffer.get(outData);
            FileUtils.writeContent(outData);
            FileUtils.writeBytes(outData);
            mMediaCodec.releaseOutputBuffer(index, false);
        }
    }


    public void stop() {
        // 释放
        isStart = false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                codec(true);
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
                eglEnv.release();
                eglEnv = null;
                mSurface = null;
                mHandler.getLooper().quitSafely();
                mHandler = null;
            }
        });
    }
}
