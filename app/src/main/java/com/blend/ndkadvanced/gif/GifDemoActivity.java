package com.blend.ndkadvanced.gif;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.R;
import com.blend.ndkadvanced.databinding.ActivityGifDemoBinding;

import java.io.File;

public class GifDemoActivity extends AppCompatActivity {

    private static final String TAG = "GifDemoActivity";

    private ActivityGifDemoBinding mBinding;
    private GifHandler mGifHandler;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_demo);
        mBinding = ActivityGifDemoBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.gifImageBtn.setOnClickListener(v -> ndkLoadGif());
    }


    public void ndkLoadGif() {
        File file = new File(Environment.getExternalStorageDirectory(), "test.gif");
        mGifHandler = GifHandler.load(file.getAbsolutePath());
        int width = mGifHandler.getWidth();
        int height = mGifHandler.getHeight();
        Log.i("BlendAndroid", "宽: " + width + "   高: " + height);

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //通知C渲染创建的bitmap
        int delay = mGifHandler.updateFrame(bitmap);
        mBinding.gifImage.setImageBitmap(bitmap);
        mHandler.sendEmptyMessageDelayed(1, delay);
    }

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            // 重新渲染下一帧
            int delay = mGifHandler.updateFrame(bitmap);
            mHandler.sendEmptyMessageDelayed(1, delay);
            mBinding.gifImage.setImageBitmap(bitmap);
            return true;
        }
    });
}