package com.blend.ndkadvanced.h264;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.utils.FileUtils;

import java.nio.ByteBuffer;

// 需要注意的是，使用 Surface 进行视频编码或解码时，需要保证 Surface 的尺寸与视频的尺寸相匹配，否则可能会导致图像变形或者无法正常播放。
public class ScreenShortService extends Service {

    private static final String TAG = "ScreenShortService";

    private ScreenShortBinder mShareBinder;
    private Notification mNotification;

    private static final String CHANNEL_ID = "H264视频编解码";
    private static final int NOTIFICATION_ID = TAG.hashCode();

    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        mShareBinder = new ScreenShortBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mShareBinder;
    }

    private void startForeground() {
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, getNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                stopForeground(true);
                startForeground(NOTIFICATION_ID, getNotification());
            }

        } else {
            startForeground(NOTIFICATION_ID, getNotification());
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_ID, importance);
            channel.setDescription(CHANNEL_ID);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        if (mNotification == null) {
            Intent notificationIntent = new Intent(getApplicationContext(), H264Activity.class);
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("屏幕录制")
                    .setContentIntent(pendingIntent)
                    .setContentText("点击返回屏幕录制");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }
            mNotification = builder.build();
        }
        return mNotification;
    }

    public class ScreenShortBinder extends Binder {
        public ScreenShortService getService() {
            return ScreenShortService.this;
        }
    }


    private MediaProjection mediaProjection;

    private MediaCodec mediaCodec;

    private VirtualDisplay mVirtualDisplay;

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        initMediaCodec();
    }

    private void initMediaCodec() {
        try {
            // 创建编码H264
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            // 描述视频轨道的 MediaFormat 对象
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                    540, 960);
            // 用于指定视频的像素格式, 用于指定使用 Surface 作为输入或输出媒体数据的像素格式
            // Surface 中的图像数据直接传递给编码器或解码器，而不需要进行任何图像格式的转换操作，从而减少了 CPU 的使用量和数据传输的延迟。
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

            // 帧率
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            // 码率
            format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
            // I帧的间隔,也会根据场景来改变
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);//2s一个I帧

            //编码配置, 表示该组件将被用于编码或解码数据
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            // 通过mediaCodec拿到Surface,是一个虚拟的,用于将图像数据传递给编码器进行编码
            final Surface surface = mediaCodec.createInputSurface();

            // 编码是耗时操作
            new Thread() {
                @Override
                public void run() {
                    // 开始编码
                    mediaCodec.start();
                    isRunning = true;

                    //mediaCodec与MediaProjection进行关联,创建一个虚拟的展示,数据会放在surface里面
                    mVirtualDisplay = mediaProjection.createVirtualDisplay("screen-codec",
                            540, 960, 1,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                            surface, null, null);

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                    while (true) {
                        if (!isRunning) {
                            break;
                        }

                        // 拿到解码后的数据，是通过输出缓冲区，返回他的索引，在10ms内解码
                        // 用于从编码器或解码器的输出缓冲区队列中取出一个可用的输出缓冲区，并返回其索引号
                        // 输出缓冲区
                        int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

                        Log.i(TAG, "mediaCodec run: " + index);
                        if (index >= 0) {

                            // 取出来的就是压缩数据,就是h264的流
                            ByteBuffer buffer = mediaCodec.getOutputBuffer(index);

                            // 不能直接使用,需要重新new一个byte
                            byte[] outData = new byte[bufferInfo.size];
                            buffer.get(outData);

                            FileUtils.writeContent(outData);  //以字符串的方式写入

                            FileUtils.writeBytes(outData);

                            mediaCodec.releaseOutputBuffer(index, false);
                        }

                    }

                    mediaCodec.stop();
                    mediaCodec.reset();
                    mVirtualDisplay.release();

                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        isRunning = false;

    }
}
