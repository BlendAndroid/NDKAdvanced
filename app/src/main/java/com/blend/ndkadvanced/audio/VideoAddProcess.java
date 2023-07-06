package com.blend.ndkadvanced.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoAddProcess {

    private static final String TAG = "VideoAddProcess";

    //合成视频,不需要编码,但是编辑,剪辑是需要的
    public static void appendVideo(String inputPath1, String inputPath2, String outputPath) throws IOException {
        // 新建轨道管理
        MediaMuxer mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        // 开始解析数据
        MediaExtractor videoExtractor1 = new MediaExtractor();
        videoExtractor1.setDataSource(inputPath1);

        MediaExtractor videoExtractor2 = new MediaExtractor();
        videoExtractor2.setDataSource(inputPath2);

        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        // 第一个视频文件的音视频索引
        int sourceVideoTrack1 = -1;
        int sourceAudioTrack1 = -1;

        // 总时长
        long file1_duration = 0L;

        for (int index = 0; index < videoExtractor1.getTrackCount(); index++) {
            MediaFormat format = videoExtractor1.getTrackFormat(index);
            // 第一个视频的长度
            file1_duration = format.getLong(MediaFormat.KEY_DURATION);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack1 = index;
                // 添加视频轨道
                videoTrackIndex = mediaMuxer.addTrack(format);
            } else if (mime.startsWith("audio/")) {
                sourceAudioTrack1 = index;
                // 添加音频轨道
                audioTrackIndex = mediaMuxer.addTrack(format);
            }
        }

        // 第二个视频文件的音视频索引
        int sourceVideoTrack2 = -1;
        int sourceAudioTrack2 = -1;
        for (int index = 0; index < videoExtractor2.getTrackCount(); index++) {
            MediaFormat format = videoExtractor2.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                sourceVideoTrack2 = index;
            } else if (mime.startsWith("audio/")) {
                sourceAudioTrack2 = index;
            }
        }

        mediaMuxer.start();

        int sampleSize;

        //1.write first video track into muxer.
        videoExtractor1.selectTrack(sourceVideoTrack1);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
            // 写入视频信息,还没有写入pps和sps
            // byte[] data = new byte[buffer.remaining()];
            // buffer.get(data);
            // FileUtils.writeBytes(data);
            // FileUtils.writeContent(data);

            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor1.getSampleFlags();
            info.presentationTimeUs = videoExtractor1.getSampleTime();
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor1.advance();
        }

        //2.write first audio track into muxer.
        // 解除音频轨道
        videoExtractor1.unselectTrack(sourceVideoTrack1);
        // 选择音频轨道
        videoExtractor1.selectTrack(sourceAudioTrack1);

        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
            // byte[] data = new byte[buffer.remaining()];
            // buffer.get(data);
            // FileUtils.writeBytes(data);
            // FileUtils.writeContent(data);
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor1.getSampleFlags();
            info.presentationTimeUs = videoExtractor1.getSampleTime();
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            videoExtractor1.advance();
        }

        //3.write second video track into muxer.
        videoExtractor2.selectTrack(sourceVideoTrack2);
        // 重新实例化,防止之前有数据
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor2.getSampleFlags();
            // 加上第一个视频的长度
            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
            videoExtractor2.advance();
        }

        //4.write second audio track into muxer.
        videoExtractor2.unselectTrack(sourceVideoTrack2);
        videoExtractor2.selectTrack(sourceAudioTrack2);
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500 * 1024);
        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
            info.offset = 0;
            info.size = sampleSize;
            info.flags = videoExtractor2.getSampleFlags();
            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
            videoExtractor2.advance();
        }

        videoExtractor1.release();
        videoExtractor2.release();

        // 在这里加上pps和sps
        mediaMuxer.stop();
        mediaMuxer.release();

    }
}
