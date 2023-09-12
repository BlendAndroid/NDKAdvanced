package com.blend.ndkadvanced.opengl.picture;

public enum Filter {

    // 颜色是用包含四个浮点的向量vec4表示，四个浮点分别表示RGBA四个通道，取值范围为0.0-1.0
    // 这里的数组是3个浮点数表示，没有a
    NONE(0, new float[]{0.0f, 0.0f, 0.0f}),
    GRAY(1, new float[]{0.299f, 0.587f, 0.114f}),
    COOL(2, new float[]{0.0f, 0.0f, 0.1f}),
    WARM(2, new float[]{0.1f, 0.1f, 0.0f}),
    BLUR(3, new float[]{0.006f, 0.004f, 0.002f}),
    MAGN(4, new float[]{0.0f, 0.0f, 1.0f});


    private int vChangeType;
    private float[] data;

    Filter(int vChangeType, float[] data) {
        this.vChangeType = vChangeType;
        this.data = data;
    }

    public int getType() {
        return vChangeType;
    }

    public float[] data() {
        return data;
    }

}
