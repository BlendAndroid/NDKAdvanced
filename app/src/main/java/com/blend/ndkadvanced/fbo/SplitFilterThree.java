package com.blend.ndkadvanced.fbo;

import android.content.Context;

import com.blend.ndkadvanced.R;

public class SplitFilterThree extends AbstractFilter {

    public SplitFilterThree(Context context) {
        super(context, R.raw.fbo_base_vert, R.raw.split3_screen);
    }

    public void beforeDraw() {
    }
}