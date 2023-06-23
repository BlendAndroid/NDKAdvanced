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

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

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
            // 编码的宽高
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                    540, 960);
            // 输入一个surface
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

            // 帧率
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            // 码率
            format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
            // I帧的间隔,也会根据场景来改变
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);//2s一个I帧

            //编码配置
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            // 通过mediaCodec拿到Surface,是一个虚拟的,用于拿到
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
                        int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

                        Log.i(TAG, "mediaCodec run: " + index);
                        if (index >= 0) {

                            // 取出来的就是压缩数据,就是h264的流
                            ByteBuffer buffer = mediaCodec.getOutputBuffer(index);

                            // 不能直接使用,需要重新new一个byte
                            byte[] outData = new byte[bufferInfo.size];
                            buffer.get(outData);

                            writeContent(outData);  //以字符串的方式写入

                            writeBytes(outData);

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

    private void writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        Log.i(TAG, "writeContent: " + sb.toString());
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(getBaseContext().getExternalCacheDir() + "/codec.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(getBaseContext().getExternalCacheDir() + "/codec.h264", true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
