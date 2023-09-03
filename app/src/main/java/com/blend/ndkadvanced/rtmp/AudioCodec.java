package com.blend.ndkadvanced.rtmp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.blend.ndkadvanced.utils.DefaultPoolExecutor;

import java.nio.ByteBuffer;

// AudioRecord采集pcm硬编码为aac
public class AudioCodec implements Runnable {
    private static final String TAG = "AudioCodec";

    private MediaCodec mediaCodec;

    private int minBufferSize;
    private boolean isRecoding;
    private AudioRecord audioRecord;
    private long startTime;
    //传输层
    private final ScreenLiveService screenLive;

    public AudioCodec(ScreenLiveService screenLive) {
        this.screenLive = screenLive;
    }

    public void startLive() {
        try {
            minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);

            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);

            // 录音质量
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //一秒的码率 aac
            format.setInteger(MediaFormat.KEY_BIT_RATE, 64_000);
            // 一定要这是这个代码,否则inputBuffer.put(buffer);会报java.nio.BufferOverflowException的错误,
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            if (ActivityCompat.checkSelfPermission(screenLive, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        DefaultPoolExecutor.getInstance().execute(this);
    }

    @Override
    public void run() {
        isRecoding = true;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        // 在音频编码前，需要先编码音频的头部信息，音频特殊配置数据 ( AAC : Audio Specific config ) , 指导后续音频采样如何解码
        // 音频编码参数为：
        // aacObjectType:AAC_LC,对应值为2，用5bit二进制表示为00010;
        // sampleRate:44100KHz, 对应的IDX值为4, 用4bit二进制表示为0100;
        // numChannels:2，对应的值为2，用4bit二进制表示为0010；
        // 将它们由高位到低位串起来：0001,0010,0001,0000，
        // 则，对应的十六进制值为:0x1220
        // 因为这里是单筒但，所以numChannels为1，所以算下来就是0x1208
        // 这个不是可以播放的数据
        RTMPPackage rtmpPackage = new RTMPPackage();
        byte[] audioDecoderSpecificInfo = {0x12, 0x08};
        rtmpPackage.setBuffer(audioDecoderSpecificInfo);
        rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_HEAD);
        screenLive.addPackage(rtmpPackage);

        // 开始录音
        Log.i(TAG, "开始录音  minBufferSize： " + minBufferSize);
        audioRecord.startRecording();
        // 容器固定大小
        byte[] buffer = new byte[minBufferSize];
        while (isRecoding) {
            // 麦克风的数据读取出来
            int len = audioRecord.read(buffer, 0, buffer.length);

            // pcm 数据编码
            Log.i(TAG, "开始录音  len： " + len);

            if (len <= 0) {
                continue;
            }
            // 立即得到有效输入缓冲区
            int index = mediaCodec.dequeueInputBuffer(1_000);
            if (index >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                Log.i(TAG, "开始录音  inputBuffer.limit()： " + inputBuffer.limit());
                inputBuffer.put(buffer);
                inputBuffer.limit(len);
                //填充数据后再加入队列
                mediaCodec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0);
            }

            index = mediaCodec.dequeueOutputBuffer(bufferInfo, 1_000);
            while (index >= 0 && isRecoding) {
                // 得到输出缓冲区
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData); //编码好的数据

                // 音频的数据在编码后封包。RTMPPackage需要一个时间，这个时间一般是相对时间，先给出第一帧的时间，后面的时间都是相对于第一帧的时间
                // startTime == 0 说明是第一帧，记录一下开始时间
                if (startTime == 0) {
                    startTime = bufferInfo.presentationTimeUs / 1000;   // 绝对时间
                }

                rtmpPackage = new RTMPPackage();
                rtmpPackage.setBuffer(outData);
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_DATA);
                // 设置时间戳，相对时间
                long tms = (bufferInfo.presentationTimeUs / 1000) - startTime;
                rtmpPackage.setTms(tms);
                screenLive.addPackage(rtmpPackage);

                mediaCodec.releaseOutputBuffer(index, false);
                index = mediaCodec.dequeueOutputBuffer(bufferInfo, 1_000);
            }
        }
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        startTime = 0;
    }

    public void stopLive() {
        isRecoding = false;
    }
}
