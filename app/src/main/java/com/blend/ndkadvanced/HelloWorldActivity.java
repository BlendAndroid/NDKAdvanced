package com.blend.ndkadvanced;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityHelloWorldBinding;
import com.blend.ndkadvanced.databinding.ActivityMainBinding;

public class HelloWorldActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityHelloWorldBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityHelloWorldBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // Example of a call to a native method
        TextView tv = mBinding.sampleText;
        tv.setText(stringFromJNI());
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}