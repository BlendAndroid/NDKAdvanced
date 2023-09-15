package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.opengl.GLES20;

import com.blend.ndkadvanced.R;

public class SoulFilter extends AbstractFilter {
    private final int scalePercent;
    private final int mixturePercent;

    float scale = 0.0f; //缩放，越大就放的越大
    float mix = 0.0f; //透明度，越大越透明

    public SoulFilter(Context context) {
        super(context, R.raw.fbo_base_vert, R.raw.soul_frag);
        scalePercent = GLES20.glGetUniformLocation(program, "scalePercent");
        mixturePercent = GLES20.glGetUniformLocation(program, "mixturePercent");
    }

    @Override
    public void beforeDraw() {
        // 缩放是从以0.08f,最大缩放到2倍,从1-2不断的变化
        GLES20.glUniform1f(scalePercent, scale + 1.0f);
        // 透明度,也是0.08f,从1-0不断的变化
        GLES20.glUniform1f(mixturePercent, 1.0f - mix);
        scale += 0.08f;
        mix += 0.08f;
        if (scale >= 1.0) {
            scale = 0.0f;
        }
        if (mix >= 1.0) {
            mix = 0.0f;
        }
    }
}
