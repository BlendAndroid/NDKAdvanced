package com.blend.ndkadvanced.fbo;

import android.content.Context;

import com.blend.ndkadvanced.R;

public class SplitFilterTwo extends AbstractFilter {

    public SplitFilterTwo(Context context) {
        super(context, R.raw.fbo_base_vert, R.raw.split2_screen);
    }

    public void beforeDraw() {
    }
}