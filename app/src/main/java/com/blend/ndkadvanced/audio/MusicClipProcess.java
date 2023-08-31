package com.blend.ndkadvanced.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.blend.ndkadvanced.utils.FileUtils;
import com.blend.ndkadvanced.utils.PcmToWavUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class MusicClipProcess {

    private static final String TAG = "MusicClipProcess";


    /**
     * 裁剪音频
     *
     * @param musicPath 原始音频
     * @param outPath   裁剪后的音频
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @throws Exception 异常
     */
    @SuppressLint("WrongConstant")
    public void clip(String musicPath, String outPath, int startTime, int endTime) throws Exception {
        if (endTime < startTime) {
            return;
        }

        // 对封装格式解封装的类
        // 从多媒体文件中提取音频和视频数据的类。读取数据轨道的信息以及每个样本包含的数据
        // MediaExtractor可以读取多种音频和视频文件格式
        MediaExtractor mediaExtractor = new MediaExtractor();

        // 设置封装格式路径
        mediaExtractor.setDataSource(musicPath);

        // 找到音频轨道
        int audioTrack = selectTrack(mediaExtractor);

        // 设置需要解封装的音频轨道
        mediaExtractor.selectTrack(audioTrack);

        // 设置需要裁剪的开始时间
        // seekTo()方法的第二个参数是一个常量，表示seek的模式，有三种模式：
        // SEEK_TO_PREVIOUS_SYNC：寻找上一个关键帧
        // SEEK_TO_NEXT_SYNC：寻找下一个关键帧
        // SEEK_TO_CLOSEST_SYNC：寻找距离给定的时间戳最近的关键帧
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

        // 获取轨道信息
        MediaFormat oriAudioFormat = mediaExtractor.getTrackFormat(audioTrack);

        // 设置输出缓冲区buffer的大小
        int maxBufferSize;
        if (oriAudioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = oriAudioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);

        // 是audio/mpeg格式
        Log.e(TAG, "KEY_MIME: " + oriAudioFormat.getString((MediaFormat.KEY_MIME)));

        // 设置解码类,oriAudioFormat.getString就是获取解码的格式
        MediaCodec mediaCodec = MediaCodec.createDecoderByType(oriAudioFormat.getString((MediaFormat.KEY_MIME)));

        // 根据封装格式,设置解码器信息
        mediaCodec.configure(oriAudioFormat, null, null, 0);

        // 设置解码后的输出地址
        File pcmFile = new File(outPath, "out.pcm");
        FileChannel writeChannel = new FileOutputStream(pcmFile).getChannel();
        // 开始解码
        mediaCodec.start();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outputBufferIndex = -1;
        while (true) {
            int decodeInputIndex = mediaCodec.dequeueInputBuffer(100000);
            if (decodeInputIndex >= 0) {

                // 获取当前解码的时间戳,getSampleTime()方法获取该样本的时间戳
                long sampleTimeUs = mediaExtractor.getSampleTime();

                if (sampleTimeUs == -1) {
                    break;
                } else if (sampleTimeUs < startTime) {
                    // 如果解码的时间戳小于开始时间,移动到下一个样本
                    mediaExtractor.advance();
                    continue;
                } else if (sampleTimeUs > endTime) {
                    // 如果解码的时间戳大于结束时间,就不用结束
                    break;
                }
                // 获取到压缩数据,记录到BufferInfo
                // 从媒体文件中读取当前样本的数据并将其存储到指定的ByteBuffer中.返回值为样本数据的长度，如果返回值小于0，则表示已经读取完了所有的样本数据。
                // 用于读取媒体文件中的音视频数据，读取的数据存储在ByteBuffer中，返回值为读取的数据的长度，如果返回值小于0，则表示已经读取完了所有的数据。
                // readSampleData()方法有两个参数，第一个参数是一个ByteBuffer对象，用于存储读取的数据；第二个参数是一个int类型的参数，表示要读取的数据的起始位置。readSampleData()方法会将读取的数据存储在ByteBuffer对象中，并返回读取的数据的大小，单位为字节。
                // 注意，readSampleData()方法读取的数据是一帧音视频数据，而不是整个音视频轨道。因此，在处理读取的数据时需要注意帧与帧之间的分割。
                info.size = mediaExtractor.readSampleData(buffer, 0);
                info.presentationTimeUs = sampleTimeUs;
                info.flags = mediaExtractor.getSampleFlags();

                // 将数据放入到dsp解码
                byte[] content = new byte[buffer.remaining()];
                buffer.get(content);
                // 输出文件,这里输出的都是AAC格式的,就是FFF开头的
                FileUtils.writeContent(content);

                //解码从MediaExtractor中读取的数据
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(decodeInputIndex);
                inputBuffer.put(content);
                mediaCodec.queueInputBuffer(decodeInputIndex, 0, info.size, info.presentationTimeUs, info.flags);
                // 前进到下一个样本
                mediaExtractor.advance();
            }

            // 获取解码后的数据, 就是PCM数据
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            while (outputBufferIndex >= 0) {
                ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                // 解码后的数据,就是pcm数据
                writeChannel.write(decodeOutputBuffer);
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            }
        }
        writeChannel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();

        // 将pcm数据转换成mp3封装格式,将PCM转换成WAV格式
        File wavFile = new File(outPath, "output.mp3");
        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(pcmFile.getAbsolutePath()
                , wavFile.getAbsolutePath());
        Log.i(TAG, "clip: 转换完毕");
    }

    private int selectTrack(MediaExtractor mediaExtractor) {
        // 获取音频,视频的轨道数量,如音频轨,视频轨
        int numTracks = mediaExtractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            // 获取轨道的配置信息
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            // 获取轨道的信息
            String mime = format.getString(MediaFormat.KEY_MIME);
            // 如果是音频
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;


    }
}
