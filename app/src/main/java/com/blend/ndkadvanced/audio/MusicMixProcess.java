package com.blend.ndkadvanced.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.blend.ndkadvanced.utils.PcmToWavUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class MusicMixProcess {

    private static final String TAG = "MusicMixProcess";

    /**
     * 将视频和音频的声音，混合音频
     *
     * @param context     上下文
     * @param videoInput  视频的输入路径
     * @param audioInput  音频的输入路径
     * @param output      输出mp3路径
     * @param startTimeUs 开始时间
     * @param endTimeUs   结束时间
     * @param videoVolume 视频声音大小
     * @param aacVolume   音频声音大小
     * @throws Exception
     */
    public void mixAudioTrack(Context context,
                              final String videoInput,
                              final String audioInput,
                              final String output,
                              final Integer startTimeUs, final Integer endTimeUs,
                              int videoVolume,
                              int aacVolume
    ) throws Exception {

        final File videoPcmFile = new File(Environment.getExternalStorageDirectory(), "video.pcm");
        final File musicPcmFile = new File(Environment.getExternalStorageDirectory(), "music.pcm");

        decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

        decodeToPCM(audioInput, musicPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);


        final File mixPcmFile = new File(Environment.getExternalStorageDirectory(), "mix.pcm");
        // 混合PCM
        mixPcm(videoPcmFile.getAbsolutePath(), musicPcmFile.getAbsolutePath(), mixPcmFile.getAbsolutePath(), videoVolume, aacVolume);

        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixPcmFile.getAbsolutePath()
                , output);
    }

    /**
     * 将mp3转换成pcm,并截取
     *
     * @param musicPath
     * @param outPath
     * @param startTime
     * @param endTime
     * @throws Exception
     */
    @SuppressLint("WrongConstant")
    private void decodeToPCM(String musicPath, String outPath, int startTime, int endTime) throws Exception {
        if (endTime < startTime) {
            return;
        }
        MediaExtractor mediaExtractor = new MediaExtractor();

        mediaExtractor.setDataSource(musicPath);
        int audioTrack = selectTrack(mediaExtractor);

        mediaExtractor.selectTrack(audioTrack);
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        MediaFormat oriAudioFormat = mediaExtractor.getTrackFormat(audioTrack);
        int maxBufferSize;
        if (oriAudioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = oriAudioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        // 这里就能解析视频,音频
        MediaCodec mediaCodec = MediaCodec.createDecoderByType(oriAudioFormat.getString((MediaFormat.KEY_MIME)));
        mediaCodec.configure(oriAudioFormat, null, null, 0);
        File pcmFile = new File(outPath);
        FileChannel writeChannel = new FileOutputStream(pcmFile).getChannel();
        mediaCodec.start();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outputBufferIndex;
        while (true) {
            int decodeInputIndex = mediaCodec.dequeueInputBuffer(100000);
            if (decodeInputIndex >= 0) {
                long sampleTimeUs = mediaExtractor.getSampleTime();

                if (sampleTimeUs == -1) {
                    break;
                } else if (sampleTimeUs < startTime) {
                    mediaExtractor.advance();
                    continue;
                } else if (sampleTimeUs > endTime) {
                    break;
                }
                info.size = mediaExtractor.readSampleData(buffer, 0);
                info.presentationTimeUs = sampleTimeUs;
                info.flags = mediaExtractor.getSampleFlags();

                byte[] content = new byte[buffer.remaining()];
                buffer.get(content);

                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(decodeInputIndex);
                inputBuffer.put(content);
                mediaCodec.queueInputBuffer(decodeInputIndex, 0, info.size, info.presentationTimeUs, info.flags);
                mediaExtractor.advance();
            }

            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 100_000);
            while (outputBufferIndex >= 0) {
                ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                writeChannel.write(decodeOutputBuffer);
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 100_000);
            }
        }
        writeChannel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();

        Log.i(TAG, "decodeToPCM: 转换完毕");
    }

    private int selectTrack(MediaExtractor mediaExtractor) {
        int numTracks = mediaExtractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }


    private float normalizeVolume(int volume) {
        return volume / 100f * 1;
    }

    // 开始混音
    private void mixPcm(String pcm1Path, String pcm2Path, String toPath
            , int volume1, int volume2) throws IOException {

        // 防止精度丢失
        float vol1 = normalizeVolume(volume1);
        float vol2 = normalizeVolume(volume2);

        //每次的读取的文件，2K
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];

        //待输出数据
        byte[] buffer3 = new byte[2048];

        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);

        //输出PCM 的
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);

        short temp2, temp1;
        // 两个short变量相加，会大于short，所以定义为int
        int temp;

        // 结束标志位
        boolean end1 = false, end2 = false;

        while (!end1 || !end2) {

            if (!end1) {
                // 将pcm1数据读取到buffer1
                end1 = (is1.read(buffer1) == -1);
                // 将buffer1的数据写入到 buffer3
                System.arraycopy(buffer1, 0, buffer3, 0, buffer1.length);

            }

            if (!end2) {
                // 将pcm2数据读取到buffer2
                end2 = (is2.read(buffer2) == -1);

                // 一个音频的值是两个字节，+2的原因就是跳过一个声音
                for (int i = 0; i < buffer2.length; i += 2) {
                    // 计算音频值，将低八位的值放入到高八位的后面
                    temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);
                    temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                    // 合成，调节两个音频大小
                    temp = (int) (temp1 * vol1 + temp2 * vol2);

                    // 超过音频的最大值和最小值
                    if (temp > 32767) {
                        temp = 32767;
                    } else if (temp < -32768) {
                        temp = -32768;
                    }

                    // 先存储低八位
                    buffer3[i] = (byte) (temp & 0xFF);
                    // 再存储高八位
                    buffer3[i + 1] = (byte) ((temp >>> 8) & 0xFF);
                }
                fileOutputStream.write(buffer3);
            }
        }
        is1.close();
        is2.close();
        fileOutputStream.close();
    }
}
