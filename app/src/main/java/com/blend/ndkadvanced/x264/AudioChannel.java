package com.blend.ndkadvanced.x264;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class AudioChannel {
    private final LivePusher livePusher;
    private final int channelConfig;
    private int minBufferSize;
    private final byte[] buffer;
    private final Handler handler;
    private HandlerThread handlerThread;
    private AudioRecord audioRecord;

    @SuppressLint("MissingPermission")
    public AudioChannel(int sampleRate, int channels, LivePusher livePusher) {
        this.livePusher = livePusher;
        // 使用双通道
        channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        // 返回成功创建 AudioRecord 对象所需的最小缓冲区大小（以字节为单位）
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

        // 初始化faac软编, 获取输入缓冲区大小
        int inputByteNum = livePusher.init_audioEnc(sampleRate, channels);

        buffer = new byte[inputByteNum];
        // 选择两者中的较大值
        minBufferSize = Math.max(inputByteNum, minBufferSize);

        // 开启子线程
        handlerThread = new HandlerThread("Audio-Record");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // 初始化录音
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
    }

    public void start() {
        // 在子线程编码
        handler.post(new Runnable() {
            @Override
            public void run() {
                // 开始录音
                audioRecord.startRecording();
                // 表示当前正在录制音频,输出的PCM的数据
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    // len实际长度len 打印下这个值
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    // Log.i("rtmp", "len: " + len);
                    if (len > 0) {
                        // 通道数是2, 位深是16, int32_t是16的2陪,所有长度得除以2
                        // 计算PCM音频文件大小: 采样率 * 位深 * 通道数 * 时间(秒)
                        livePusher.native_sendAudio(buffer, len / 2);
                    }
                }
            }
        });

    }

    public void stop() {
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    public void release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }
}
