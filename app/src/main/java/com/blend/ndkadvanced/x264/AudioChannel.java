package com.blend.ndkadvanced.x264;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class AudioChannel {
    private LivePusher livePusher;
    private int sampleRate;
    private int channelConfig;
    private int minBufferSize;
    private byte[] buffer;
    private Handler handler;
    private HandlerThread handlerThread;
    private AudioRecord audioRecord;

    @SuppressLint("MissingPermission")
    public AudioChannel(int sampleRate, int channels, LivePusher livePusher) {
        this.livePusher = livePusher;
        this.sampleRate = sampleRate;
//        双通道应该传的值   一律用单通道
        channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO :
                AudioFormat.CHANNEL_IN_MONO;
//        数据大小  是根据  mediacodec  来的   数据   怎么不准确 minBufferSize 参考 值    软编  肯定返回  硬编   不行


//        硬编   不可以    minBufferSize  -1
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT);
//        初始化faac软编  inputByteNum  最小容器
        int inputByteNum = livePusher.init_audioEnc(sampleRate, channels);
        buffer = new byte[inputByteNum];
//        输入容器  java  层   录音类 读取出来   输入容器 java
        minBufferSize = Math.max(inputByteNum, minBufferSize);
        handlerThread = new HandlerThread("Audio-Record");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        //                读取麦克风的数据
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
    }

    public void start() {
//        子线程编码
        handler.post(new Runnable() {
            @Override
            public void run() {
//开始录音
                audioRecord.startRecording();
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
//                    len实际长度len 打印下这个值  录音不成功
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    Log.i("rtmp", "len: " + len);
                    if (len > 0) {
                        // 通道数是16, int32_t是16的2陪,所有长度得除以2
                        livePusher.native_sendAudio(buffer, len / 2);
                    }
                }
            }
        });

    }

    public void stop() {
        audioRecord.startRecording();
    }
}
