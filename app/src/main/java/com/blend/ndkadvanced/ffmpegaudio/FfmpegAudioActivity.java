package com.blend.ndkadvanced.ffmpegaudio;

import static com.blend.ndkadvanced.ffmpegaudio.musicservice.MusicService.ACTION_OPT_MUSIC_VOLUME;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.databinding.ActivityFfmpegAudioBinding;
import com.blend.ndkadvanced.ffmpegaudio.musicservice.MusicService;
import com.blend.ndkadvanced.ffmpegaudio.musicui.model.MusicData;
import com.blend.ndkadvanced.ffmpegaudio.musicui.utils.DisplayUtil;
import com.blend.ndkadvanced.ffmpegaudio.musicui.widget.DiscView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FfmpegAudioActivity extends AppCompatActivity implements DiscView.IPlayInfo, View.OnClickListener {

    private static final String TAG = "FfmpegAudioActivity";

    private ActivityFfmpegAudioBinding mBinding;

    public static final String PARAM_MUSIC_LIST = "PARAM_MUSIC_LIST";
    DisplayUtil displayUtil = new DisplayUtil();
    private MusicReceiver mMusicReceiver = new MusicReceiver();
    private List<MusicData> mMusicDatas = new ArrayList<>();
    private int totalTime;
    private int position;
    private boolean playState = false;

    public static final int DURATION_NEEDLE_ANIAMTOR = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityFfmpegAudioBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initMusicDatas();
        initView();
        initMusicReceiver();
        DisplayUtil.makeStatusBarTransparent(this);
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    optMusic(ACTION_OPT_MUSIC_VOLUME);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        }
        return false;
    }

    private void initMusicReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAY);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PAUSE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_DURATION);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_COMPLETE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAYER_TIME);
        /*注册本地广播*/
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver, intentFilter);
    }

    private void initView() {
        mBinding.discview.discView.setPlayInfoListener(this);
        mBinding.ivLast.setOnClickListener(this);
        mBinding.ivNext.setOnClickListener(this);
        mBinding.ivPlayOrPause.setOnClickListener(this);
        mBinding.musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = totalTime * progress / 100;
                mBinding.tvCurrentTime.setText(displayUtil.duration2Time(progress));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "onStopTrackingTouch: " + position);
                seekTo(position);
            }
        });
        mBinding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        mBinding.tvTotalTime.setText(displayUtil.duration2Time(0));
        mBinding.discview.discView.setMusicDataList(mMusicDatas);
    }

    private void playCurrentTime(int currentTime, int totalTime) {
        mBinding.musicSeekBar.setProgress(currentTime * 100 / totalTime);
        this.totalTime = totalTime;
        mBinding.tvCurrentTime.setText(DisplayUtil.secdsToDateFormat(currentTime, totalTime));
        mBinding.tvTotalTime.setText(DisplayUtil.secdsToDateFormat(totalTime, totalTime));
    }

    private void initMusicDatas() {
        MusicData musicData1 = new MusicData(R.raw.ffmpeg_music1, R.raw.ffmpeg_ic_music1, "等你归来", "程响");
        MusicData musicData2 = new MusicData(R.raw.ffmpeg_music2, R.raw.ffmpeg_ic_music2, "Nightingale", "YANI");
        MusicData musicData3 = new MusicData(R.raw.ffmpeg_music3, R.raw.ffmpeg_ic_music3, "Cornfield Chase", "Hans Zimmer");
        mMusicDatas.add(musicData1);
        mMusicDatas.add(musicData2);
        mMusicDatas.add(musicData3);
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(PARAM_MUSIC_LIST, (Serializable) mMusicDatas);
        startService(intent);
    }

    @Override
    public void onMusicInfoChanged(String musicName, String musicAuthor) {
        getSupportActionBar().setTitle(musicName);
        getSupportActionBar().setSubtitle(musicAuthor);
    }

    @Override
    public void onMusicPicChanged(int musicPicRes) {
        displayUtil.try2UpdateMusicPicBackground(this, mBinding.rootLayout, musicPicRes);
    }

    @Override
    public void onMusicChanged(DiscView.MusicChangedStatus musicChangedStatus) {
        switch (musicChangedStatus) {
            case PLAY: {
                play();
                break;
            }
//            case PAUSE:{
//                pause();
//                break;
//            }
            case NEXT: {
                next();
                break;
            }
            case LAST: {
                last();
                break;
            }
            case STOP: {
                stop();
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBinding.ivPlayOrPause) {
            playState = !playState;
            Log.i(TAG, "onClick: ---------" + playState);
            if (playState) {
                mBinding.ivPlayOrPause.setImageResource(R.drawable.ic_play);
                pause();
                mBinding.discview.discView.stop();
            } else {
                mBinding.ivPlayOrPause.setImageResource(R.drawable.ic_pause);
                resume();
                mBinding.discview.discView.play();
            }


        } else if (v == mBinding.ivNext) {
            mBinding.discview.discView.next();
        } else if (v == mBinding.ivLast) {
            mBinding.discview.discView.last();
        }
    }

    private void play() {
        optMusic(MusicService.ACTION_OPT_MUSIC_PLAY);
    }

    private void pause() {
        optMusic(MusicService.ACTION_OPT_MUSIC_PAUSE);
    }

    public void resume() {
        optMusic(MusicService.ACTION_OPT_MUSIC_RESUME);
    }

    private void stop() {
        mBinding.ivPlayOrPause.setImageResource(R.drawable.ic_play);
        mBinding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        mBinding.tvTotalTime.setText(displayUtil.duration2Time(0));
        mBinding.musicSeekBar.setProgress(0);
    }

    private void next() {
        mBinding.rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                optMusic(MusicService.ACTION_OPT_MUSIC_NEXT);
            }
        }, DURATION_NEEDLE_ANIAMTOR);
        mBinding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        mBinding.tvTotalTime.setText(displayUtil.duration2Time(0));
    }

    private void last() {
        mBinding.rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                optMusic(MusicService.ACTION_OPT_MUSIC_LAST);
            }
        }, DURATION_NEEDLE_ANIAMTOR);
        mBinding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        mBinding.tvTotalTime.setText(displayUtil.duration2Time(0));
    }

    private void complete(boolean isOver) {
        if (isOver) {
            mBinding.discview.discView.stop();
        } else {
            mBinding.discview.discView.next();
        }
    }

    private void optMusic(final String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
    }

    private void seekTo(int position) {
        Intent intent = new Intent(MusicService.ACTION_OPT_MUSIC_SEEK_TO);
        intent.putExtra(MusicService.PARAM_MUSIC_SEEK_TO, position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicService.ACTION_STATUS_MUSIC_PLAY)) {
                mBinding.ivPlayOrPause.setImageResource(R.drawable.ic_pause);
                int currentPosition = intent.getIntExtra(MusicService.PARAM_MUSIC_CURRENT_POSITION, 0);
                mBinding.musicSeekBar.setProgress(currentPosition);
                if (!mBinding.discview.discView.isPlaying()) {
                    mBinding.discview.discView.playOrPause();
                }
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_PAUSE)) {
                mBinding.ivPlayOrPause.setImageResource(R.drawable.ic_play);
                if (mBinding.discview.discView.isPlaying()) {
                    mBinding.discview.discView.playOrPause();
                }
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_DURATION)) {
                int duration = intent.getIntExtra(MusicService.PARAM_MUSIC_DURATION, 0);
//                updateMusicDurationInfo(duration);
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_COMPLETE)) {
                boolean isOver = intent.getBooleanExtra(MusicService.PARAM_MUSIC_IS_OVER, true);
                complete(isOver);
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_PLAYER_TIME)) {
                int currentTime = intent.getIntExtra("currentTime", 0);
                int totalTime = intent.getIntExtra("totalTime", 0);
                playCurrentTime(currentTime, totalTime);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver);
    }

    public void left(View view) {

        optMusic(MusicService.ACTION_OPT_MUSIC_LEFT);
    }

    public void right(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_RIGHT);
    }

    public void center(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_CENTER);
    }

    public void speed(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_SPEED_AN_NO_PITCH);
    }

    public void pitch(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_SPEED_NO_AN_PITCH);
    }

    public void speedpitch(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_SPEED_AN_PITCH);
    }

    public void normalspeedpitch(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_SPEED_PITCH_NOMAORL);

    }
}