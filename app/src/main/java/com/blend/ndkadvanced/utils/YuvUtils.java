package com.blend.ndkadvanced.utils;

public class YuvUtils {
    private static final String TAG = "YuvUtils";

    static byte[] nv12;

    public static byte[] nv21toNV12(byte[] nv21) {
        int size = nv21.length;
        nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);
        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    // 将横屏的数据，旋转成竖屏的数据
    public static void portraitData2Raw(byte[] data, byte[] output, int width, int height) {
        int y_len = width * height;
        int uvHeight = height >> 1; // uv数据高为y数据高的一半
        int k = 0;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                output[k++] = data[width * i + j];
            }
        }
        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                output[k++] = data[y_len + width * i + j];
                output[k++] = data[y_len + width * i + j + 1];
            }
        }
    }
}
