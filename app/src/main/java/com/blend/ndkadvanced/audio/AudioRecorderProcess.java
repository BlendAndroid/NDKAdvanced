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
            isRecording = true;
            int minBufferSize = AudioRecord.getMinBufferSize(44100, 1, AudioFormat.ENCODING_PCM_16BIT);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
            if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "initRecord: mAudioRecord init failed");
                isRecording = false;
                return;
            }
            mRecorder.startRecording();

            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();

            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            byte[] bytesBuffer = new byte[minBufferSize];
            int len = 0;
            while (isRecording && (len = mRecorder.read(bytesBuffer, 0, minBufferSize)) > 0) {
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

                int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    int outBitsSize = mBufferInfo.size;
                    int outPacketSize = outBitsSize + 7; // ADTS头部是7个字节
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + outBitsSize);

                    byte[] outData = new byte[outPacketSize];
                    addADTStoPacket(outData, outPacketSize);

                    outputBuffer.get(outData, 7, outBitsSize);
                    outputBuffer.position(mBufferInfo.offset);
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
