package com.blend.ndkadvanced.fbo;

import android.content.Context;

import com.blend.ndkadvanced.R;

public class RecordFilter extends AbstractFilter{
    public RecordFilter(Context context){
        super(context, R.raw.fbo_base_vert, R.raw.fbo_base_frag);
    }

    @Override
    public void beforeDraw() {

    }
}
