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
                mBinding.fboCameraView.startRecord();
            }

            @Override
            public void onRecordStop() {
                mBinding.btnFboRecord.setBackgroundResource(R.drawable.record_button_background_unselected);
                mBinding.fboCameraView.stopRecord();
            }
        });

        mBinding.fboRgSpeed.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.fboExtraSlow:
                mBinding.fboCameraView.setSpeed(CameraView.Speed.MODE_EXTRA_SLOW);
                break;
            case R.id.fboSlow:
                mBinding.fboCameraView.setSpeed(CameraView.Speed.MODE_SLOW);
                break;
            case R.id.btnFboNormal:
                mBinding.fboCameraView.setSpeed(CameraView.Speed.MODE_NORMAL);
                break;
            case R.id.fboFast:
                mBinding.fboCameraView.setSpeed(CameraView.Speed.MODE_FAST);
                break;
            case R.id.fboExtraFast:
                mBinding.fboCameraView.setSpeed(CameraView.Speed.MODE_EXTRA_FAST);
                break;
        }
    }
}