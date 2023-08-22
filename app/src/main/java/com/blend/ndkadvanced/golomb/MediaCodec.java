package com.blend.ndkadvanced.golomb;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MediaCodec {

    private static final String TAG = "MediaCodec";

    private final String path;

    public MediaCodec(String path) {
        this.path = path;
    }

    public void startCodec() {
        byte[] bytes = null;
        try {
            bytes = getBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bytes == null) {
            return;
        }
        int totalSize = bytes.length;
        int startIndex = 0;
        while (true) {
            if (totalSize == 0 || startIndex >= totalSize) {
                break;
            }

            // 获取下一帧的索引
            int nextFrameStart = findByFrame(bytes, startIndex + 2, totalSize);

            // 拿到sps和pps的头
            byte[] h264 = spliteByte(bytes, startIndex, nextFrameStart - startIndex);
            // 跳过开始分隔符
            nStartBit = 4 * 8;
            // 禁止位，初始为0，当NAL单元有比特错误时将该值为1，以便接收方纠错或者丢弃
            int forbidden_zero_bit = u(1, h264);
            // NAL单元的重要性，值越大，越重要
            int nal_ref_idc = u(2, h264);
            // 宏块类型
            int nal_unit_type = u(5, h264);
            switch (nal_unit_type) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 7:
                    //进入sps解码过程
                    parseSps(h264);
                    break;
                case 8:
                    break;
            }
            break;
        }


    }

    private void parseSps(byte[] h264) {

        // 编码等级   Baseline Main Extended High   High 10   High 4:2:2
        int profile_idc = u(8, h264);
        // 当constrained_set0_flag值为1的时候，就说明码流应该遵循基线profile(Baseline profile)的所有约束.constrained_set0_flag值为0时，说明码流不一定要遵循基线profile的所有约束。
        int constraint_set0_flag = u(1, h264);//(h264[1] & 0x80)>>7;
        // 当constrained_set1_flag值为1的时候，就说明码流应该遵循主profile(Main profile)的所有约束.constrained_set1_flag值为0时，说明码流不一定要遵
        int constraint_set1_flag = u(1, h264);//(h264[1] & 0x40)>>6;
        // 当constrained_set2_flag值为1的时候，就说明码流应该遵循扩展profile(Extended profile)的所有约束.constrained_set2_flag值为0时，说明码流不一定要遵循扩展profile的所有约束。
        int constraint_set2_flag = u(1, h264);//(h264[1] & 0x20)>>5;
        // 注意：当constraint_set0_flag,constraint_set1_flag或constraint_set2_flag中不只一个值为1的话，那么码流必须满足所有相应指明的profile约束。
        int constraint_set3_flag = u(1, h264);//(h264[1] & 0x10)>>4;
        // 4个零位
        int reserved_zero_4bits = u(4, h264);
        // 它指的是码流对应的level级
        int level_idc = u(8, h264);
        // 是否是哥伦布编码  0 是 1 不是
        int seq_parameter_set_id = Ue(h264);
        if (profile_idc == 100) {
            // 颜色位数
            int chroma_format_idc = Ue(h264);
            int bit_depth_luma_minus8 = Ue(h264);
            int bit_depth_chroma_minus8 = Ue(h264);
            int qpprime_y_zero_transform_bypass_flag = u(1, h264);
            int seq_scaling_matrix_present_flag = u(1, h264);
        }
        int log2_max_frame_num_minus4 = Ue(h264);
        int pic_order_cnt_type = Ue(h264);
        int log2_max_pic_order_cnt_lsb_minus4 = Ue(h264);
        int num_ref_frames = Ue(h264);
        int gaps_in_frame_num_value_allowed_flag = u(1, h264);
        int pic_width_in_mbs_minus1 = Ue(h264);
        int pic_height_in_map_units_minus1 = Ue(h264);

        // 这里获取到的就是宏块的个数，因此需要乘以16，其单位是像素
        int width = (pic_width_in_mbs_minus1 + 1) * 16;
        int height = (pic_height_in_map_units_minus1 + 1) * 16;
        Log.e(TAG, "width: " + width + "  height: " + height);
    }

    // 截取数组
    private byte[] spliteByte(byte[] array, int start, int lenght) {
        byte[] newArray = new byte[lenght];
        for (int i = start; i < start + lenght; i++) {
            newArray[i - start] = array[i];
        }
        return newArray;
    }


    private int findByFrame(byte[] bytes, int start, int totalSize) {
        for (int i = start; i < totalSize; i++) {
            if ((bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x00
                    && bytes[i + 3] == 0x01) || (bytes[i] == 0x00 && bytes[i + 1] == 0x00
                    && bytes[i + 2] == 0x01)) {
                return i;
            }
        }
        return -1;  // Not found
    }

    private byte[] getBytes(String path) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024 * 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        len = is.read(buf, 0, size);
        bos.write(buf, 0, len);
        buf = bos.toByteArray();
        return buf;
    }

    // 正常的解码
    private static int u(int bitIndex, byte[] h264) {
        int dwRet = 0;
        for (int i = 0; i < bitIndex; i++) {
            dwRet <<= 1;
            if ((h264[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0) {
                dwRet += 1;
            }
            nStartBit++;
        }
        return dwRet;
    }

    private static int nStartBit = 0;


    // 哥伦布解码
    private static int Ue(byte[] pBuff) {
        int nZeroNum = 0;
        while (nStartBit < pBuff.length * 8) {
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0) {
                break;
            }
            nZeroNum++;
            nStartBit++;
        }
        nStartBit++;

        int dwRet = 0;
        for (int i = 0; i < nZeroNum; i++) {
            dwRet <<= 1;
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0) {
                dwRet += 1;
            }
            nStartBit++;
        }
        return (1 << nZeroNum) - 1 + dwRet;
    }

}
