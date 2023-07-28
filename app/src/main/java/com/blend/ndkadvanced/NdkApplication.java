package com.blend.ndkadvanced;

import android.app.Application;

public class NdkApplication extends Application {

    public static NdkApplication application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = new NdkApplication();
    }

    public static NdkApplication getApplication() {
        return application;
    }

}
