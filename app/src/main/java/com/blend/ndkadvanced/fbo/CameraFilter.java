package com.blend.ndkadvanced.fbo;

import android.content.Context;
import android.opengl.GLES20;

import com.blend.ndkadvanced.R;

public class CameraFilter extends AbstractFboFilter {

    private float[] mtx;
    private final int vMatrix;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vert, R.raw.camera_frag2);
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");
    }

    @Override
    public void beforeDraw() {
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
    }

    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }
}
