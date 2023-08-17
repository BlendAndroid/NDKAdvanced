package com.blend.ndkadvanced.opengl;

import static com.blend.ndkadvanced.opengl.base.BaseOpenGlLActivity.BASE_OPEN_GL_ACTIVITY;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityOpenGlBinding;
import com.blend.ndkadvanced.opengl.base.BaseOpenGlLActivity;
import com.blend.ndkadvanced.opengl.picture.PictureFilterActivity;
import com.blend.ndkadvanced.opengl.picture.PictureFilterRender;

public class OpenGLActivity extends AppCompatActivity {

    private ActivityOpenGlBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityOpenGlBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBinding.btnOpenGLBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBaseType();
            }
        });

        mBinding.btnOpenGLPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OpenGLActivity.this, PictureFilterActivity.class));
            }
        });
    }

    private void selectBaseType() {
        String[] selectPicTypeStr = {"三角形", "三角形2", "带颜色的三角形", "正方形", "圆形", "立方体", "圆锥", "纹理贴图之显示图片"};
        new AlertDialog.Builder(this)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(OpenGLActivity.this, "取消", Toast.LENGTH_SHORT).show();
                    }
                })
                .setItems(selectPicTypeStr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Intent intent = new Intent(OpenGLActivity.this, BaseOpenGlLActivity.class);
                        intent.putExtra(BASE_OPEN_GL_ACTIVITY, which);
                        startActivity(intent);
                    }
                })
                .show();
    }
}