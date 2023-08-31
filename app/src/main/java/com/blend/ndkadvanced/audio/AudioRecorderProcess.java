package com.blend.ndkadvanced.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.blend.ndkadvanced.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 声音有两个基本属性：频率与振幅。声音的振幅就是音量，频率的高低就是音调，频率的单位是赫兹（Hz）。
 * <p>
 * PCM元数据
 * 最常见的A/D转换是通过脉冲编码调制 PCM (Pulse Code Modulation)。要将连续的电压信号转换为PCM，需要进行采样和量化，
 * 我们一般从如下几个维度描述PCM：
 * <p>
 * 采样频率（Sampling Rate）：单位时间内采集的样本数，即：采样周期的倒数，指两个采样之间的时间间隔。采样频率越高，声音质量越好，
 * 但同时占用的带宽越大。一般情况下，22KHz相当于普通FM的音质，44KHz相当于CD音质，目前的常用采样频率都不超过48KHz。
 * <p>
 * 采样位数：表示一个样本的二进制位数，即：每个采样点用多少比特表示。计算机中音频的量化深度一般为4、8、16、32位（bit）等。
 * 例如：采样位数为8 bit时，每个采样点可以表示256个不同的采样值，而采样位数为16 bit时，每个采样点可以表示65536个不同的
 * 采样值。采样位数的大小影响声音的质量，采样位数越多，量化后的波形越接近原始波形，声音的质量越高，而需要的存储空间也越多；
 * 位数越少，声音的质量越低，需要的存储空间越少。一般情况下，CD音质的采样位数是16 bit，移动通信是8 bit。
 * <p>
 * 声道数：记录声音时，如果每次生成一个声波数据，称为单声道；每次生成两个声波数据，称为双声道（立体声）。单声道的声音只能
 * 使用一个喇叭发声，双声道的PCM可以使两个喇叭同时发声（一般左右声道有分工），更能感受到空间效果。
 * <p>
 * 时长：采样时长,数字音频文件大小（Byte) = 采样频率（Hz）× 采样时长（S）×（采样位数 / 8）× 声道数（单声道为1，立体声为2）
 * 采样点数据有有符号和无符号之分，比如：8 bit的样本数据，有符号的范围是-128 ~ 127，无符号的范围是0 ~ 255。大多数PCM样本
 * 使用整形表示，但是在一些对精度要求比较高的场景，可以使用浮点类型表示PCM样本数据。
 * <p>
 * PCM (Pulse Code Modulation) 是一种数字音频编码方法，它将模拟音频信号转换为数字化的音频数据流，是一种无损压缩的编码方式。
 * 而 AAC（Advanced Audio Coding）是一种有损压缩的音频编码格式，是一种被广泛应用于数字音频广播、数字电视、DVD、蓝光光盘和
 * 流媒体等领域的音频编码方式。
 * <p>
 * AAC编码使用了一些先进的音频压缩技术，能够比PCM编码更好地压缩音频数据，在保证高音质的同时，大大减小了音频文件的文件大小，这使得AAC
 * 编码成为了数字音频领域的主流标准。
 * <p>
 * 实际上，AAC可以被认为是对PCM数据的再编码。在将音频数据压缩为AAC格式之前，首先需要将音频数据采样为PCM格式，然后使用各种编码技术
 * 将PCM数据转换为AAC格式。因此，AAC可以视为一种基于PCM的音频编码格式。
 */
public class AudioRecorderProcess {

    private static final String TAG = "AudioRecorderProcess";

    private AudioRecord mRecorder;
    private MediaCodec mEncoder;
    private boolean isRecording;
    private final Context context;

    public AudioRecorderProcess(Context context) {
        this.context = context;
    }

    public void start() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            isRecording = true;
            // 采样率：44100Hz，单声道，16bit采样
            int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
            if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "initRecord: mAudioRecord init failed");
                isRecording = false;
                return;
            }
            mRecorder.startRecording();

            // 当前输入的是AAC音频数据
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            // AACObjectLC常量就表示低复杂度AAC音频编码配置，它的值为2, 可以提供较高的编码效率和压缩比，同时适合低码率（例如64kbps）的音频压缩。
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            // 设置音频比特率, 96kbps是一个很好的选择,一般在64kbps-128kbps之间
            format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();

            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            byte[] bytesBuffer = new byte[minBufferSize];
            int len = 0;
            while (isRecording && (len = mRecorder.read(bytesBuffer, 0, minBufferSize)) > 0) {
                // 将音频数据放入dsp芯片中进行编码
                int inputBufferIndex = mEncoder.dequeueInputBuffer(100000);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(bytesBuffer);
                    inputBuffer.limit(len);
                    mEncoder.queueInputBuffer(inputBufferIndex, 0, len, System.nanoTime(), 0);
                } else {
                    isRecording = false;
                }

                // 获取解码后的数据
                int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    int outBitsSize = mBufferInfo.size;
                    int outPacketSize = outBitsSize + 7; // ADTS头部是7个字节

                    // 原始数据在outputBuffer中
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
                    // 这个offset都是0
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + outBitsSize);

                    // 添加ADTS头部
                    byte[] outData = new byte[outPacketSize];
                    addADTStoPacket(outData, outPacketSize);

                    // 将outputBuffer的数据,放入outData的第7个字节之后,大小为outBitsSize
                    outputBuffer.get(outData, 7, outBitsSize);
                    outputBuffer.position(mBufferInfo.offset);

                    // 保存到文件中
                    FileUtils.writeContent(outData);
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                }
            }
            release();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRecording = false;
    }

    private void release() throws IOException {
        if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        if (mEncoder != null) {
            mEncoder.stop();
        }
    }

    // ADTS是Advanced Audio Coding（AAC）音频文件格式的一种封装格式, 全称是Audio Data Transport Stream，即音频数据传输流。
    // ADTS格式可以将音频数据流分为一个一个的帧，每个帧都包含了一个ADTS头和一个AAC原始音频数据包。ADTS头中包含了一些与AAC音频有关
    // 的配置信息，例如采样率、比特率、声道数等参数，同时也包含了一些控制信息，例如同步字等。
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44100
        int chanCfg = 2;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
