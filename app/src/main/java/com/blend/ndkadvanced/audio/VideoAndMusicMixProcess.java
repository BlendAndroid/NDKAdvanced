package com.blend.ndkadvanced.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.blend.ndkadvanced.utils.PcmToWavUtil;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoAndMusicMixProcess {

    private static final String TAG = "VideoAndMusicMixProcess";

    private static final int TIMEOUT = 1000;

    /**
     * 视频添加背景音乐
     *
     * @param context     上下文
     * @param videoInput  视频文件
     * @param audioInput  音频文件
     * @param output      输出文件
     * @param startTimeUs 开始时间
     * @param endTimeUs   结束时间
     * @param videoVolume 视频音量
     * @param aacVolume   音频音量
     * @throws Exception
     */
    public static void mixAudioTrack(Context context, final String videoInput, final String audioInput, final String output, final Integer startTimeUs, final Integer endTimeUs, int videoVolume, int aacVolume) throws Exception {

        File cacheDir = context.getExternalCacheDir();
        // 音频的pcm
        File aacPcmFile = new File(cacheDir, "audio" + ".pcm");
        // 视频原生pcm
        File videoPcmFile = new File(cacheDir, "video" + ".pcm");

        MusicMixProcess.decodeToPCM(audioInput, aacPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

        MusicMixProcess.decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

        // 混合后的视频pcm
        File adjustedPcm = new File(cacheDir, "mix" + ".pcm");
        MusicMixProcess.mixPcm(videoPcmFile.getAbsolutePath(), aacPcmFile.getAbsolutePath(), adjustedPcm.getAbsolutePath(), videoVolume, aacVolume);

        // PCM转换成WAV
        File wavFile = new File(cacheDir, adjustedPcm.getName() + ".wav");
        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(adjustedPcm.getAbsolutePath(), wavFile.getAbsolutePath());
        Log.i(TAG, "mixAudioTrack: 转换完毕");

        // 混音的wav文件 + 视频文件   --->  生成新的视频文件
        mixVideoAndMusic(videoInput, wavFile, output, startTimeUs, endTimeUs);
    }


    /**
     * 步骤是将视频文件拆分成音频轨和视频轨
     * 音频轨使用混合后的wav文件，将wav文件通过dsp编码成aac格式，然后添加到视频的音频轨
     * 视频轨使用原视频文件，只需要通过MediaExtractor读取视频轨的数据，然后添加到新的视频文件中
     *
     * @param videoInput  视频文件
     * @param wavFile     音频混合后的文件
     * @param output      输出文件
     * @param startTimeUs 开始时间
     * @param endTimeUs   结束时间
     * @throws Exception 异常
     */
    public static void mixVideoAndMusic(String videoInput, File wavFile, String output, Integer startTimeUs, Integer endTimeUs) throws Exception {

        // 初始化一个视频封装容器，MediaMuxer可以将多个音频轨道和视频轨道合并为一个文件，并将它们的时间轴同步，同时可以添加字幕和元数据等信息。
        MediaMuxer mediaMuxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        // 提取视频信息
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(videoInput);
        // 拿到视频轨道的索引
        int videoIndex = MusicMixProcess.selectTrack(mediaExtractor, false);
        // 拿到音频轨道的索引
        int audioIndex = MusicMixProcess.selectTrack(mediaExtractor, true);
        // 获取视频配置信息
        MediaFormat videoFormat = mediaExtractor.getTrackFormat(videoIndex);

        // 向输出文件中添加一个新的空的视频轨道，新轨道的格式信息由MediaFormat指定，如编码格式、采样率、帧率
        mediaMuxer.addTrack(videoFormat);

        // 视频中音频轨道, 应该取自于原视频的音频参数
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioIndex);
        // 获取视频音频的比特率
        int audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        // 设置视频中音频的编码格式为AAC
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        // 添加一个新的空的音频轨道，轨道格式取自视频文件，跟视频的音频信息一样，返回新的音频轨道的索引
        int muxerAudioIndex = mediaMuxer.addTrack(audioFormat);

        // 轨道开辟好了，启动混合器，然后通过writeSampleData()将数据写入文件中
        mediaMuxer.start();

        //提取音乐音频信息
        MediaExtractor pcmExtractor = new MediaExtractor();
        pcmExtractor.setDataSource(wavFile.getAbsolutePath());
        int audioTrack = MusicMixProcess.selectTrack(pcmExtractor, true);
        // 选择音乐音频轨道
        pcmExtractor.selectTrack(audioTrack);
        // 获取音乐音频的配置信息
        MediaFormat pcmTrackFormat = pcmExtractor.getTrackFormat(audioTrack);
        //最大一帧的 大小
        int maxBufferSize = 0;
        if (pcmTrackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = pcmTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }

        // 设置音频格式
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);//参数对应-> mime type、采样率、声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);
        // 创建一个音频编码器
        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        // 配置AAC参数
        encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        // 开辟一个音频的最大容器
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean encodeDone = false;
        while (!encodeDone) {
            int inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT);
            if (inputBufferIndex >= 0) {
                // 获取当前解码的时间戳,getSampleTime()方法获取该样本的时间戳
                long sampleTime = pcmExtractor.getSampleTime();

                if (sampleTime < 0) {
                    // pts小于0，来到了文件末尾，通知编码器，不用编码了
                    encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    int flags = pcmExtractor.getSampleFlags();
                    int size = pcmExtractor.readSampleData(buffer, 0);

                    // 将读取的音频数据放入dsp中进行编码
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(buffer);
                    inputBuffer.position(0);

                    encoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags);
                    // 移动到下一个样本
                    pcmExtractor.advance();
                }
            }

            // 获取编码完的数据
            int outputBufferIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            while (outputBufferIndex >= 0) {
                // 如果已经到了文件末尾
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    encodeDone = true;
                    break;
                }
                ByteBuffer encodeOutputBuffer = encoder.getOutputBuffer(outputBufferIndex);
                // 将编码好的数据放入封装容器的音频轨道，也就是视频的音频轨道
                mediaMuxer.writeSampleData(muxerAudioIndex, encodeOutputBuffer, info);
                encodeOutputBuffer.clear();
                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            }
        }

        // 音乐音频添加完，释放音乐音频轨道
        if (audioTrack >= 0) {
            pcmExtractor.unselectTrack(audioTrack);
        }

        // 开始添加视频
        mediaExtractor.selectTrack(videoIndex);

        mediaExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        buffer = ByteBuffer.allocateDirect(maxBufferSize);

        //封装容器添加视频轨道信息
        while (true) {
            long sampleTimeUs = mediaExtractor.getSampleTime();
            if (sampleTimeUs == -1) {
                break;
            }

            // 不到开始时间,丢弃
            if (sampleTimeUs < startTimeUs) {
                mediaExtractor.advance();
                continue;
            }

            // 大于结束时间
            if (endTimeUs != null && sampleTimeUs > endTimeUs) {
                break;
            }

            // 不从0开始
            info.presentationTimeUs = sampleTimeUs - startTimeUs + 600;
            info.flags = mediaExtractor.getSampleFlags();

            // 读取视频文件的数据,就是压缩数据
            info.size = mediaExtractor.readSampleData(buffer, 0);
            if (info.size < 0) {
                break;
            }
            // 视频轨道，画面写完了
            mediaMuxer.writeSampleData(videoIndex, buffer, info);
            mediaExtractor.advance();
        }

        try {
            pcmExtractor.release();
            mediaExtractor.release();
            encoder.stop();
            encoder.release();
            mediaMuxer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
