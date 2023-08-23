package com.blend.ndkadvanced.screenshare.push;

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
import com.blend.ndkadvanced.socket.PushSocketLive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;

public class PushScreenShareService extends Service {

    private static final String TAG = "PushScreenShareService";

    private PushScreenShareBinder mPushShareBinder;
    private Notification mNotification;

    private static final String CHANNEL_ID = "H265屏幕共享推流";
    private static final int NOTIFICATION_ID = TAG.hashCode();

    private MediaProjection mMediaProjection;
    private VirtualDisplay virtualDisplay;
    private PushSocketLive mPushSocketLive;

    private MediaCodec mediaCodec;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        mPushShareBinder = new PushScreenShareBinder();
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
            Intent notificationIntent = new Intent(getApplicationContext(), PushScreenShareActivity.class);
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("屏幕共享推流端")
                    .setContentIntent(pendingIntent)
                    .setContentText("点击返回屏幕共享");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }
            mNotification = builder.build();
        }
        return mNotification;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mPushShareBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaCodec.stop();
        mediaCodec.reset();
        virtualDisplay.release();
    }

    public class PushScreenShareBinder extends Binder {
        public PushScreenShareService getService() {
            return PushScreenShareService.this;
        }
    }

    public void init(PushSocketLive pushSocketLive, MediaProjection mediaProjection) {
        this.mPushSocketLive = pushSocketLive;
        this.mMediaProjection = mediaProjection;
    }

    public void startPush() {
        try {
            int width = 720;
            int height = 1280;
            // video/hevc就是H265
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            // 将Surface作为视频编码的输出
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(KEY_BIT_RATE, width * height);
            format.setInteger(KEY_FRAME_RATE, 20);
            format.setInteger(KEY_I_FRAME_INTERVAL, 1);
            mediaCodec = MediaCodec.createEncoderByType("video/hevc");
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            //创建场地
            virtualDisplay = mMediaProjection.createVirtualDisplay(
                    "-display", width, height, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);

        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaCodec.start();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                while (true) {
                    try {
                        int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outputBufferId >= 0) {
                            //编码好的H265的数据
                            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferId);

                            dealFrame(byteBuffer, bufferInfo);

                            mediaCodec.releaseOutputBuffer(outputBufferId, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }

                }
            }
        }).start();
    }

    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    private byte[] vps_sps_pps_buf;

    // 每一帧的前面得加上vps，sps和pps
    // 一个ByteBuffer就是一个NAL单元
    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        // 偏移量设置是4
        int offset = 4;
        // 00 00 01也是开头
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        // 0x7Ed0  0x7E是1111110
        int type = (bb.get(offset) & 0x7E) >> 1;
        if (type == NAL_VPS) {  // 获取到VPS,SPS,PPS
            vps_sps_pps_buf = new byte[bufferInfo.size];
            bb.get(vps_sps_pps_buf);
            Log.e(TAG, "VPS,SPS,PPS数据: " + Arrays.toString(vps_sps_pps_buf));
        } else if (type == NAL_I) { // 每一个I帧的前面加上VPS,SPS,PPS

            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);

            byte[] newBuf = new byte[vps_sps_pps_buf.length + bytes.length];
            System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.length, bytes.length);
            this.mPushSocketLive.sendData(newBuf);
            Log.e(TAG, "I帧视频数据: " + Arrays.toString(newBuf));
        } else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.mPushSocketLive.sendData(bytes);
            Log.e(TAG, "非I帧视频数据: " + Arrays.toString(bytes));
        }

    }

}
