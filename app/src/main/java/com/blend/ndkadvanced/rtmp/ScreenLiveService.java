package com.blend.ndkadvanced.rtmp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.utils.DefaultPoolExecutor;

import java.util.concurrent.LinkedBlockingQueue;

//推送层   维持这样的队列
public class ScreenLiveService extends Service implements Runnable {

    private static final String TAG = "ScreenLive";

    private MediaProjection mediaProjection;

    // 队列
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();

    // 正在执行     isLive    关闭
    private boolean isLiving;

    private String url;

    private VideoCodec videoCodec;

    private AudioCodec audioCodec;

    private ScreenLiveBinder mShareBinder;

    private Notification mNotification;

    private static final String CHANNEL_ID = "RTMP向B站推流";
    private static final int NOTIFICATION_ID = TAG.hashCode();

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        mShareBinder = new ScreenLiveBinder();
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
            Intent notificationIntent = new Intent(getApplicationContext(), RTMPActivity.class);
            PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification.Builder builder = new Notification.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("正在向B站推流")
                    .setContentIntent(pendingIntent)
                    .setContentText("点击返回屏幕录制");
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
        return mShareBinder;
    }

    protected class ScreenLiveBinder extends Binder {
        public ScreenLiveService getService() {
            return ScreenLiveService.this;
        }
    }

    // 开启RTMP推送
    public void startLive(String url, MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        this.url = url;
        DefaultPoolExecutor.getInstance().execute(this);
    }

    @Override
    public void run() {
        //1推送到
        if (!connect(url)) {
            Log.i(TAG, "run: ----------->推送失败");
            return;
        }
//        开启线程
//
        videoCodec = new VideoCodec(ScreenLiveService.this);
        videoCodec.startLive(mediaProjection);

        audioCodec = new AudioCodec(ScreenLiveService.this);
        audioCodec.startLive();

        isLiving = true;
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i(TAG, "run: ----------->推送 " + rtmpPackage.getBuffer().length);

                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length, rtmpPackage.getTms(), rtmpPackage.getType());
            }


//            消费者
        }
    }

    //生产者入口
    public void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }

    public void stopLive() {
        isLiving = false;
        if (videoCodec != null) {
            videoCodec.stopLive();
        }
        if (audioCodec != null) {
            audioCodec.stopLive();
        }
    }

    private native boolean sendData(byte[] data, int len, long tms, int type);

    private native boolean connect(String url);

}
