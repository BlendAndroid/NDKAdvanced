package com.blend.ndkadvanced.golomb;

import android.util.Log;

// 哥伦布编码是一个可变长编码
class GolombDemo {

    private static final String TAG = "GolombDemo";

    /**
     * 哥伦布编码的原理
     * 求哥伦布编码是5的原始值是多少
     * 00000101
     */
    public static void Ue() {
        // 开始的时候是第三位
        int nStartBit = 3;

        // 将int值的5改成byte上的5
        byte data = 5 & 0xFF;

        //统计0 的个数
        int nZeroNum = 0;

        // 0x80是1000 0000
        while (nStartBit < 8) {
            // 进行0x80的右移运算,按位与,找到哥伦布编码的1
            // 0x80的二进制是1000 0000,右移nStartBit位,就是找到第nStartBit位是1的时候
            if ((data & (0x80 >> (nStartBit))) != 0) {
                break;
            }
            nZeroNum++;
            nStartBit++;
        }

        nStartBit++;

        // 计算哥伦布编码1后面的值
        int dwRet = 0;
        for (int i = 0; i < nZeroNum; i++) {
            // 每次循环都先左移1位
            dwRet = dwRet << 1;
            // 当找到1位不是1的时候,就加1
            if ((data & (0x80 >> (nStartBit % 8))) != 0) {
                dwRet += 1;
            }
            nStartBit++;
        }

        // 哥伦布编码之前+1,现在要减去1
        int value = (1 << nZeroNum) - 1 + dwRet;
        Log.e(TAG, "Ue: " + value);
    }
}
