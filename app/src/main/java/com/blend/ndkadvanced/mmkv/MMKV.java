package com.blend.ndkadvanced.mmkv;

import android.content.Context;

public class MMKV {

    // C++对象的地址
    private final long nativeHandle;

    private MMKV(long handle) {
        nativeHandle = handle;
    }

    public static void initialize(Context context) {
        String rootDir = context.getExternalCacheDir() + "/mmkv";
        // 创建文件夹
        jniInitialize(rootDir);
    }

    // 实例化java层的mmkv对象
    public static MMKV defaultMMKV() {
        // 获取C++对象的地址
        long handle = getDefaultMMKV();
        return new MMKV(handle);
    }

    public void putInt(String key, int value) {
        putInt(nativeHandle, key, value);
    }

    public int getInt(String key, int defaultValue) {
        return getInt(nativeHandle, key, defaultValue);
    }

    private static native void jniInitialize(String rootDir);

    private native static long getDefaultMMKV();

    private native void putInt(long handle, String key, int value);

    private native int getInt(long handle, String key, int defaultValue);


}
