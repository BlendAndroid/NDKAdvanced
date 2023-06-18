package com.blend.ndkadvanced.gif;

import android.graphics.Bitmap;
import android.util.Log;

public class GifHandler {

    private static final String TAG = "GifHandler";

    // 定义指针类型
    private long gifHandler;

    // 动态库导入，需要加载
    static {
        System.loadLibrary("native-lib");
    }

    // 加载gif
    public static GifHandler load(String absolutePath) {
        long gifHandler = loadGif(absolutePath);

        return new GifHandler(gifHandler);
    }

    private GifHandler(long gifHandler) {
        this.gifHandler = gifHandler;
    }

    public int getWidth() {
        return getWidth(gifHandler);
    }

    public int getHeight() {
        return getHeight(gifHandler);
    }

    public int updateFrame(Bitmap bitmap) {
        return updateFrame(gifHandler, bitmap);
    }

    // 加载Gif
    public static native long loadGif(String path);

    // 获取宽
    public static native int getWidth(long gifHandler);

    // 获取高
    public static native int getHeight(long gifHandler);

    // 渲染图片,并返回下一帧的延迟
    public static native int updateFrame(long gifPoint, Bitmap bitmap);


}
