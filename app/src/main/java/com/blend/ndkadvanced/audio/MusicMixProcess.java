package com.blend.ndkadvanced.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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
    public void mixAudioTrack(Context context, final String videoInput, final String audioInput, final String output, final Integer startTimeUs, final Integer endTimeUs, int videoVolume, int aacVolume) throws Exception {

        final File videoPcmFile = new File(context.getExternalCacheDir(), "video.pcm");
        final File musicPcmFile = new File(context.getExternalCacheDir(), "music.pcm");

        decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

        decodeToPCM(audioInput, musicPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);


        final File mixOutput = new File(context.getExternalCacheDir(), "mixOutput.pcm");

        // 混合PCM
        mixPcm(videoPcmFile.getAbsolutePath(), musicPcmFile.getAbsolutePath(), mixOutput.getAbsolutePath(), videoVolume, aacVolume);

        // 将PCM转换成了WAV,但是没有压缩,只不过换成了后缀是mp3
        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixOutput.getAbsolutePath(), output);
        Log.i(TAG, "decodeToWAV: 转换完毕");
    }

    /**
     * 将mp3转换成pcm,并截取
     *
     * @param musicOrVideoPath 音视频路径
     * @param outPath          输出路径
     * @param startTime        开始时间
     * @param endTime          结束时间
     * @throws Exception
     */
    @SuppressLint("WrongConstant")
    public static void decodeToPCM(String musicOrVideoPath, String outPath, int startTime, int endTime) throws Exception {
        if (endTime < startTime) {
            return;
        }
        MediaExtractor mediaExtractor = new MediaExtractor();

        mediaExtractor.setDataSource(musicOrVideoPath);
        int audioTrack = selectTrack(mediaExtractor, true);
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
            int decodeInputIndex = mediaCodec.dequeueInputBuffer(1_000);
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

            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            while (outputBufferIndex >= 0) {
                ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                writeChannel.write(decodeOutputBuffer);
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            }
        }
        writeChannel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();

        Log.i(TAG, "decodeToPCM: 转换完毕");
    }


    public static int selectTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }

    // 将音量转换成0-1之间的值
    private static float normalizeVolume(int volume) {
        return volume / 100f * 1;
    }

    // 开始混音
    public static void mixPcm(String pcm1Path, String pcm2Path, String toPath, int volume1, int volume2) throws IOException {

        // 防止精度丢失, 值是0 ~ 1
        float vol1 = normalizeVolume(volume1);
        float vol2 = normalizeVolume(volume2);

        // 每次的读取的文件，2K
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];

        //待输出数据
        byte[] buffer3 = new byte[2048];

        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);

        //输出PCM 的
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);

        // 这里采用的是16位PCM音频数据的编码格式，即每个采样点的值使用16个二进制位来表示,也就是两个字节，低八位存储前面,高八位存储后面
        // 所以用short表示,表示的值最高就是32767,最低就是-32768，用short表示的时候，就得把高八位提取到前面去
        // 在表示编码格式的时候,不需要关心通道数
        // PCM的数据格式,如果是单声道音频，采样数据按照时间的先后顺序依次存储，如果是双声道音频，则按照LRLRLR方式存储
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

                // 一个音频的采样值是两个字节，+2的原因就是跳过一个short,因为tmp是short类型的,是两个字节
                for (int i = 0; i < buffer2.length; i += 2) {
                    // 计算音频值，高8位在前，低8位在后
                    // 将低8位放在高8位的后面, 所以buffer1[i + 1] & 0xff) << 8就是为了左移8位
                    // 0xff是提取出8位,并转换成无符号的整数
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
        Log.i(TAG, "decodeToPCM: 混合完毕");
    }
}
