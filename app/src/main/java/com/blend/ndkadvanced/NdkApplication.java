package com.blend.ndkadvanced;

import android.app.Application;

import com.blend.ndkadvanced.utils.FileUtils;

public class NdkApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        FileUtils.init(this);
    }

}
