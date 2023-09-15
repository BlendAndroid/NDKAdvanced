package com.blend.ndkadvanced.fbo;

import android.content.Context;

import com.blend.ndkadvanced.R;

public class ScreenFilter extends AbstractFilter {
    public ScreenFilter(Context context) {
        super(context, R.raw.fbo_base_vert, R.raw.camera_frag2);
    }

    @Override
    public void beforeDraw() {

    }
}
