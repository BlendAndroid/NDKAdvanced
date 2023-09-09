package com.blend.ndkadvanced.mmkv;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityMmkvActivityBinding;

public class MMKVActivity extends AppCompatActivity {

    private ActivityMmkvActivityBinding mBinding;
    private MMKV mmkv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMmkvActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        // 创建文件夹
        MMKV.initialize(this);
        // java层mmkv对象
        mmkv = MMKV.defaultMMKV();

        mBinding.btnMMKVWrite.setOnClickListener(v -> {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                mmkv.putInt("name" + i, i);
            }
            long time = (System.currentTimeMillis() - start);
            Toast.makeText(this, "MMKV写入时间花销:" + time, Toast.LENGTH_SHORT).show();
        });

        mBinding.btnMMKVRead.setOnClickListener(v -> {
            Toast.makeText(this, "MMKV获取值:" + mmkv.getInt("name2", -1), Toast.LENGTH_SHORT).show();
        });
    }
}