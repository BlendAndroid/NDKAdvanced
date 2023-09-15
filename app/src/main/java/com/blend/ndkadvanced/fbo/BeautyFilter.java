package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.opengl.GLES20;

import com.blend.ndkadvanced.R;

public class BeautyFilter extends AbstractFilter {

    private final int width;
    private final int height;

    public BeautyFilter(Context context) {
        super(context, R.raw.fbo_base_vert, R.raw.beauty_frag);
        width = GLES20.glGetUniformLocation(program, "width");
        height = GLES20.glGetUniformLocation(program, "height");
    }

    @Override
    public void beforeDraw() {
        GLES20.glUniform1i(width, mWidth);
        GLES20.glUniform1i(height, mHeight);
    }


}
