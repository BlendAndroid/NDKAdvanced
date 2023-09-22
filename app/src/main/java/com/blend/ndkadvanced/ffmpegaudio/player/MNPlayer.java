package com.blend.ndkadvanced.ffmpegaudio.player;

import android.text.TextUtils;

import com.blend.ndkadvanced.ffmpegaudio.listener.IPlayerListener;
import com.blend.ndkadvanced.ffmpegaudio.listener.WlOnParparedListener;
import com.blend.ndkadvanced.ffmpegaudio.log.MyLog;


/**
 * Created by yangw on 2018-2-28.
 */

public class MNPlayer {
    static {
        System.loadLibrary("native-lib");
    }
    private String source;//数据源
    private WlOnParparedListener mnOnParparedListener;

    private IPlayerListener playerListener;

    public void setPlayerListener(IPlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    public MNPlayer()
    {}

    /**
     * 设置数据源
     * @param source
     */
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * 设置准备接口回调
     * @param mnOnParparedListener
     */
    public void setWlOnParparedListener(WlOnParparedListener mnOnParparedListener)
    {
        this.mnOnParparedListener = mnOnParparedListener;
    }

    public void parpared()
    {
        if(TextUtils.isEmpty(source))
        {
            MyLog.d("source not be empty");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_parpared(source);
            }
        }).start();

    }

    public void start()
    {
        if(TextUtils.isEmpty(source))
        {
            MyLog.d("source is empty");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_start();
            }
        }).start();
    }


    /**
     * c++回调java的方法
     */
    public void onCallParpared()
    {
        if(mnOnParparedListener != null)
        {
            mnOnParparedListener.onParpared();
        }
    }

    public void onCallTimeInfo(int currentTime, int totalTime)
    {
        if (playerListener == null) {
            return;
        }
        playerListener.onCurrentTime(currentTime, totalTime);
    }
    public void seek(int secds) {
        n_seek(secds);
    }
    public native void n_parpared(String source);
    public native void n_start();
    private native void n_seek(int secds);
    private native void n_resume();
    private native void n_pause();
    private native void n_mute(int mute);
    private native void n_volume(int percent);
    private native void n_speed(float speed);
    private native void n_pitch(float pitch);
    public void setSpeed(float speed) {
        n_speed(speed);

    }
    public void setVolume(int percent)
    {
        if(percent >=0 && percent <= 100)
        {
            n_volume(percent);
        }
    }

    public void stop()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_stop();
            }
        }).start();
    }

    private native void n_stop();
    public void pause() {
        n_pause();
    }
    public void setPitch(float pitch) {
        n_pitch(pitch);
    }
    public void resume() {
        n_resume();
    }

    public void setMute(int mute) {
        n_mute(mute);
    }
}
