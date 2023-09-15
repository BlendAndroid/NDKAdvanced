package com.blend.ndkadvanced.fbo;

import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.databinding.ActivityFboBinding;
import com.blend.ndkadvanced.utils.RecordButton;

public class FBOActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private ActivityFboBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityFboBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBinding.btnFboRecord.setOnRecordListener(new RecordButton.OnRecordListener() {
            @Override
            public void onRecordStart() {
                mBinding.btnFboRecord.setBackgroundResource(R.drawable.record_button_background_selected);
                mBinding.fboCameraSurfaceView.startRecord();
            }

            @Override
            public void onRecordStop() {
                mBinding.btnFboRecord.setBackgroundResource(R.drawable.record_button_background_unselected);
                mBinding.fboCameraSurfaceView.stopRecord();
            }
        });

        mBinding.fboRgSpeed.setOnCheckedChangeListener(this);
        mBinding.fboRgFilter.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.fboExtraSlow:
                mBinding.fboCameraSurfaceView.setSpeed(CameraSurfaceView.Speed.MODE_EXTRA_SLOW);
                break;
            case R.id.fboSlow:
                mBinding.fboCameraSurfaceView.setSpeed(CameraSurfaceView.Speed.MODE_SLOW);
                break;
            case R.id.btnFboNormal:
                mBinding.fboCameraSurfaceView.setSpeed(CameraSurfaceView.Speed.MODE_NORMAL);
                break;
            case R.id.fboFast:
                mBinding.fboCameraSurfaceView.setSpeed(CameraSurfaceView.Speed.MODE_FAST);
                break;
            case R.id.fboExtraFast:
                mBinding.fboCameraSurfaceView.setSpeed(CameraSurfaceView.Speed.MODE_EXTRA_FAST);
                break;
            case R.id.fboSoul:
                mBinding.fboCameraSurfaceView.setSplit(CameraSurfaceView.Split.MODE_SOUL);
                break;
            case R.id.fboBeauty:
                mBinding.fboCameraSurfaceView.setSplit(CameraSurfaceView.Split.MODE_BEAUTY);
                break;
            case R.id.fboNormal:
                mBinding.fboCameraSurfaceView.setSplit(CameraSurfaceView.Split.MODE_NORMAL);
                break;
            case R.id.fboSplit2:
                mBinding.fboCameraSurfaceView.setSplit(CameraSurfaceView.Split.MODE_SPLIT2);
                break;
            case R.id.fboSplit3:
                mBinding.fboCameraSurfaceView.setSplit(CameraSurfaceView.Split.MODE_SPLIT3);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.fboCameraSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBinding.fboCameraSurfaceView.onPause();
    }
}