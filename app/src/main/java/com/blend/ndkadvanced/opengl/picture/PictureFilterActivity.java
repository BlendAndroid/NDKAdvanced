package com.blend.ndkadvanced.opengl.picture;

import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityPictureFilterOpenGlBinding;

public class PictureFilterActivity extends AppCompatActivity {

    private ActivityPictureFilterOpenGlBinding mBinding;

    private PictureFilterRender mRender;

    private boolean isHalf = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPictureFilterOpenGlBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());


        // setEGLContextClientVersion ()设置OpenGL的版本，如果设置为2，则表示使用OpenGL2.0的渲染接口
        mBinding.pictureGlSurfaceView.setEGLContextClientVersion(2);
        // 设置渲染器
        mRender = new PictureFilterRender(this);
        mBinding.pictureGlSurfaceView.setRenderer(mRender);    //纹理贴图之滤镜

        /*渲染方式，RENDERMODE_WHEN_DIRTY表示被动渲染，只有在调用requestRender或者onResume等方法时才会进行渲染。RENDERMODE_CONTINUOUSLY表示持续渲染*/
        // 设置渲染模式为渲染模式为RENDERMODE_WHEN_DIRTY
        mBinding.pictureGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mBinding.btnOpenGLChangeHalf.setOnClickListener(v -> {
            isHalf = !isHalf;
            mRender.setHalf(isHalf);
            mBinding.pictureGlSurfaceView.requestRender();
            if (isHalf) {
                mBinding.btnOpenGLChangeHalf.setText("处理一半");
            } else {
                mBinding.pictureGlSurfaceView.requestRender();
                mBinding.btnOpenGLChangeHalf.setText("全部处理");
            }
        });


        mBinding.btnOpenGLChangeFilter.setOnClickListener(v -> {
            selectFilter();
        });
    }

    private void selectFilter() {
        String[] selectPicTypeStr = {"原图", "黑白", "冷色调", "暖色调", "模糊", "放大镜"};
        new AlertDialog.Builder(this).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(PictureFilterActivity.this, "取消", Toast.LENGTH_SHORT).show();
            }
        }).setItems(selectPicTypeStr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mRender.setFilterType(Filter.NONE);
                        break;
                    case 1:
                        mRender.setFilterType(Filter.GRAY);
                        break;
                    case 2:
                        mRender.setFilterType(Filter.COOL);
                        break;
                    case 3:
                        mRender.setFilterType(Filter.WARM);
                        break;
                    case 4:
                        mRender.setFilterType(Filter.BLUR);
                        break;
                    case 5:
                        mRender.setFilterType(Filter.MAGN);
                        break;
                }
                mBinding.pictureGlSurfaceView.requestRender();
                mBinding.btnOpenGLChangeFilter.setText(selectPicTypeStr[which]);
            }
        }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 生命周期对GLSurfaceView做处理
        mBinding.pictureGlSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBinding.pictureGlSurfaceView.onPause();
    }
}