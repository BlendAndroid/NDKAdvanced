package com.blend.ndkadvanced.h265;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.blend.ndkadvanced.databinding.ActivityH265Binding;

/**
 * H265出现的原因:
 * 视频分辨率从720p到1080P再到后面的4k 8k电视蓬勃发展,
 * 视频帧率从30帧 到60帧，再到120帧;宏快个数爆发式增长;宏快复杂度降低;运动矢量的复杂度大幅增加.
 * <p>
 * 对于宏快压缩算法还是以单个宏快进行预测式压缩，帧间预测，从2003年都没有发生过变化,也就是说H264
 * 的核心原理一直没变，当初开发编码时，不知道视频分辨率会发展的如此之快。完全超出了H264能编码的范畴.
 */
public class H265Activity extends AppCompatActivity {

    private ActivityH265Binding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityH265Binding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }
}