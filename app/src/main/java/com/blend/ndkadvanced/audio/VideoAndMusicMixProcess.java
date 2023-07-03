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
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoAndMusicMixProcess {

    private static final String TAG = "VideoAndMusicMixProcess";

    private static final int TIMEOUT = 1000;

    public static void mixAudioTrack(Context context,
                                     final String videoInput,
                                     final String audioInput,
                                     final String output,
                                     final Integer startTimeUs, final Integer endTimeUs,
                                     int videoVolume,
                                     int aacVolume
    ) throws Exception {

        File cacheDir = context.getExternalCacheDir();
//        下载下来的音乐转换城pcm
        File aacPcmFile = new File(cacheDir, "audio" + ".pcm");
//        视频自带的音乐转换城pcm
        File videoPcmFile = new File(cacheDir, "video" + ".pcm");

        MusicMixProcess.decodeToPCM(audioInput, aacPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

        MusicMixProcess.decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

//         MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//         mediaMetadataRetriever.setDataSource(audioInput);
// //        读取音乐时间
//         final int aacDurationMs = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//         mediaMetadataRetriever.release();

        // MediaExtractor audioExtractor = new MediaExtractor();
        // audioExtractor.setDataSource(audioInput);

        File adjustedPcm = new File(cacheDir, "mix" + ".pcm");
        MusicMixProcess.mixPcm(videoPcmFile.getAbsolutePath(), aacPcmFile.getAbsolutePath(),
                adjustedPcm.getAbsolutePath(), videoVolume, aacVolume);

        File wavFile = new File(cacheDir, adjustedPcm.getName() + ".wav");
        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, 2, AudioFormat.ENCODING_PCM_16BIT)
                .pcmToWav(adjustedPcm.getAbsolutePath(), wavFile.getAbsolutePath());
        Log.i(TAG, "mixAudioTrack: 转换完毕");
//混音的wav文件   + 视频文件   ---》  生成

        mixVideoAndMusic(context, videoInput, output, startTimeUs, endTimeUs, wavFile);

    }


    public static void mixVideoAndMusic(Context context, String videoInput, String output, Integer startTimeUs,
                                        Integer endTimeUs, File wavFile) throws Exception {


        //        初始化一个视频封装容器
        MediaMuxer mediaMuxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

//            一个轨道    既可以装音频 又视频   是 1 不是2
//            取音频轨道  wav文件取配置信息
//            先取视频
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(videoInput);
//            拿到视频轨道的索引
        int videoIndex = MusicMixProcess.selectTrack(mediaExtractor, false);

        int audioIndex = MusicMixProcess.selectTrack(mediaExtractor, true);


//            视频配置 文件
        MediaFormat videoFormat = mediaExtractor.getTrackFormat(videoIndex);
//开辟了一个 轨道   空的轨道   写数据     真实
        mediaMuxer.addTrack(videoFormat);

//        ------------音频的数据已准备好----------------------------
        // 视频中音频轨道, 应该取自于原视频的音频参数
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioIndex);
        int audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        // 添加一个空的轨道  轨道格式取自 视频文件，跟视频所有信息一样
        int muxerAudioIndex = mediaMuxer.addTrack(audioFormat);

//            音频轨道开辟好了  输出开始工作
        mediaMuxer.start();

        //音频的wav
        MediaExtractor pcmExtractor = new MediaExtractor();
        pcmExtractor.setDataSource(wavFile.getAbsolutePath());
        int audioTrack = MusicMixProcess.selectTrack(pcmExtractor, true);
        pcmExtractor.selectTrack(audioTrack);

        MediaFormat pcmTrackFormat = pcmExtractor.getTrackFormat(audioTrack);
        //最大一帧的 大小
        int maxBufferSize = 0;
        if (pcmTrackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = pcmTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }


//    最终输出   后面   混音   -----》     重采样   混音     这个下节课讲
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                44100, 2);//参数对应-> mime type、采样率、声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);//比特率
//            音质等级
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            解码  那段
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);
//解码 那
        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
//            配置AAC 参数  编码 pcm   重新编码     视频文件变得更小
        encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        encoder.start();
//            容器
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean encodeDone = false;
        while (!encodeDone) {
            int inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT);
            if (inputBufferIndex >= 0) {
                long sampleTime = pcmExtractor.getSampleTime();

                if (sampleTime < 0) {
//                        pts小于0  来到了文件末尾 通知编码器  不用编码了
                    encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    int flags = pcmExtractor.getSampleFlags();
//
                    int size = pcmExtractor.readSampleData(buffer, 0);
//                    编辑     行 1 还是不行 2   不要去用  空的
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(buffer);
                    inputBuffer.position(0);

                    encoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags);
//                        读完这一帧
                    pcmExtractor.advance();
                }
            }
//                获取编码完的数据
            int outputBufferIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            while (outputBufferIndex >= 0) {
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    encodeDone = true;
                    break;
                }
                ByteBuffer encodeOutputBuffer = encoder.getOutputBuffer(outputBufferIndex);
//                    将编码好的数据  压缩 1     aac
                mediaMuxer.writeSampleData(muxerAudioIndex, encodeOutputBuffer, info);
                encodeOutputBuffer.clear();
                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            }
        }

        //    把音频添加好了
        if (audioTrack >= 0) {
            mediaExtractor.unselectTrack(audioTrack);
        }


        // 开始视频
        mediaExtractor.selectTrack(videoIndex);

        //
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

            // 视频轨道  画面写完了
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
