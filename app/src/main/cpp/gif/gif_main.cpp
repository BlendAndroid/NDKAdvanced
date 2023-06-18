#include <jni.h>
#include <string>

// C中专门处理bitmap的库
#include <android/bitmap.h>

// 以C的方式加载
extern "C" {
#include "../gif/gif_lib.h"
}

#include <android/log.h>

// 单独的argb改成一个像素
#define  argb(a, r, g, b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

#define  dispose(ext) (((ext)->Bytes[0] & 0x1c) >> 2)
#define  trans_index(ext) ((ext)->Bytes[3])
#define  transparency(ext) ((ext)->Bytes[0] & 1)

// 一共8个字节
struct GifBean {
    int current_frame;  // 播放的当前帧
    int total_frame;    //总帧数
    int *delays;    //每一帧的延迟
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_loadGif(JNIEnv *env, jclass clazz, jstring path_) {
    // java的String转化为C的常量字符串，是一个字符数组，需要释放
    const char *path = env->GetStringUTFChars(path_, 0);

    int ERROR;

    __android_log_print(ANDROID_LOG_INFO, "loadGif path", "%s", path);

    // 打开gif文件，传递Error的地址
    GifFileType *gifFileType = DGifOpenFileName(path, &ERROR);

    if (gifFileType == nullptr) {
        return ERROR;
    }

    // 初始化缓冲区
    DGifSlurp(gifFileType);

    // 分配内存
    GifBean *gifBean = static_cast<struct GifBean *>(malloc(sizeof(struct GifBean)));
    // 移除脏数据
    memset(gifBean, 0, sizeof(GifBean));
    // 设置TAG，UserData是一个void类型，可以赋值给任何类型
    gifFileType->UserData = gifBean;
    // 当前帧是第一帧，也就是0
    gifBean->current_frame = 0;
    // 总帧数
    gifBean->total_frame = gifFileType->ImageCount;

    // 释放这个String，第一个参数是java的String
    env->ReleaseStringUTFChars(path_, path);

    // 返回一个指针，是long类型的，因为指针占用8个字节，long也是8个字节
    return (jlong) (gifFileType);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_getWidth(JNIEnv *env, jclass clazz, jlong gif_handler) {
    // 强转成指针类型
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_handler);
    // 返回宽度
    return gifFileType->SWidth;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_getHeight(JNIEnv *env, jclass clazz, jlong gif_handler) {
    // 强转成指针类型
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_handler);
    // 返回高度
    return gifFileType->SHeight;
}

/**
 *
 * @param gif 表示GIF文件的数据结构，常用于在程序中读取和处理GIF文件
 * @param info bitmap信息,比如宽高等
 * @param pixels 二维数组
 * @param force_dispose_1
 * @return
 */
int drawFrame(GifFileType *gif, AndroidBitmapInfo info, void *pixels, bool force_dispose_1) {

    // 表示GIF文件中颜色表的类型,如红绿蓝三色素
    GifColorType *bg;

    GifColorType *color;

    SavedImage *frame;

    ExtensionBlock *ext = 0;

    // 像素的描述,比如颜色表的数据
    GifImageDesc *frameInfo;

    // 表示GIF文件中颜色表的数据结构,其实就是像素字典,根据像素索引,在这里字典里面找应该的值
    ColorMapObject *colorMap;

    // 二维数组每一行的首地址
    int *line;

    int width, height, x, y, j, loc, n, inc, p;

    void *px;

    // 获取当前帧对象
    GifBean *gifBean = static_cast<GifBean *>(gif->UserData);

    width = gif->SWidth;

    height = gif->SHeight;

    // 通过当前帧,获取到这一帧的所有信息
    frame = &(gif->SavedImages[gifBean->current_frame]);

    //比如图像描述,GIF文件中每个图像的描述信息，包括图像的位置、颜色表等。
    frameInfo = &(frame->ImageDesc);

    if (frameInfo->ColorMap) {

        colorMap = frameInfo->ColorMap;
    } else {

        colorMap = gif->SColorMap;
    }


    bg = &colorMap->Colors[gif->SBackGroundColor];


    for (j = 0; j < frame->ExtensionBlockCount; j++) {

        if (frame->ExtensionBlocks[j].Function == GRAPHICS_EXT_FUNC_CODE) {

            ext = &(frame->ExtensionBlocks[j]);

            break;

        }

    }
    // For dispose = 1, we assume its been drawn
    px = pixels;
    if (ext && dispose(ext) == 1 && force_dispose_1 && gifBean->current_frame > 0) {
        gifBean->current_frame = gifBean->current_frame - 1,
                drawFrame(gif, info, pixels, true);
    } else if (ext && dispose(ext) == 2 && bg) {

        for (y = 0; y < height; y++) {

            line = (int *) px;

            for (x = 0; x < width; x++) {

                line[x] = argb(255, bg->Red, bg->Green, bg->Blue);

            }

            px = (int *) ((char *) px + info.stride);

        }

    } else if (ext && dispose(ext) == 3 && gifBean->current_frame > 1) {
        gifBean->current_frame = gifBean->current_frame - 2,
                drawFrame(gif, info, pixels, true);

    }
    px = pixels;
    if (frameInfo->Interlace) {

        n = 0;

        inc = 8;

        p = 0;

        px = (int *) ((char *) px + info.stride * frameInfo->Top);

        // 通过偏移量,开始是从Top等开始解压
        for (y = frameInfo->Top; y < frameInfo->Top + frameInfo->Height; y++) {

            for (x = frameInfo->Left; x < frameInfo->Left + frameInfo->Width; x++) {

                loc = (y - frameInfo->Top) * frameInfo->Width + (x - frameInfo->Left);

                if (ext && frame->RasterBits[loc] == trans_index(ext) && transparency(ext)) {

                    continue;

                }

                // frame->RasterBits[loc] 从索引拿到了红色蓝的三色值
                color = (ext && frame->RasterBits[loc] == trans_index(ext)) ? bg
                                                                            : &colorMap->Colors[frame->RasterBits[loc]];

                if (color)
                    // 通过argb的宏进行三色值的组合
                    line[x] = argb(255, color->Red, color->Green, color->Blue);

            }

            px = (int *) ((char *) px + info.stride * inc);

            n += inc;

            if (n >= frameInfo->Height) {

                n = 0;

                switch (p) {

                    case 0:

                        px = (int *) ((char *) pixels + info.stride * (4 + frameInfo->Top));

                        inc = 8;

                        p++;

                        break;

                    case 1:

                        px = (int *) ((char *) pixels + info.stride * (2 + frameInfo->Top));

                        inc = 4;

                        p++;

                        break;

                    case 2:

                        px = (int *) ((char *) pixels + info.stride * (1 + frameInfo->Top));

                        inc = 2;

                        p++;

                }

            }

        }

    } else {

        px = (int *) ((char *) px + info.stride * frameInfo->Top);

        for (y = frameInfo->Top; y < frameInfo->Top + frameInfo->Height; y++) {

            line = (int *) px;

            for (x = frameInfo->Left; x < frameInfo->Left + frameInfo->Width; x++) {

                loc = (y - frameInfo->Top) * frameInfo->Width + (x - frameInfo->Left);

                if (ext && frame->RasterBits[loc] == trans_index(ext) && transparency(ext)) {

                    continue;

                }

                color = (ext && frame->RasterBits[loc] == trans_index(ext)) ? bg
                                                                            : &colorMap->Colors[frame->RasterBits[loc]];

                if (color)

                    line[x] = argb(255, color->Red, color->Green, color->Blue);

            }

            px = (int *) ((char *) px + info.stride);

        }
    }
    GraphicsControlBlock gcb;//获取控制信息
    DGifSavedExtensionToGCB(gif, gifBean->current_frame, &gcb);
    int delay = gcb.DelayTime * 10;

    __android_log_print(ANDROID_LOG_INFO, "delay", "%d", delay);
    return delay;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blend_ndkadvanced_gif_GifHandler_updateFrame(JNIEnv *env, jclass clazz, jlong gif_point,
                                                      jobject bitmap) {
    // 强转成指针类型
    GifFileType *gifFileType = reinterpret_cast<GifFileType *>(gif_point);

    // 还有另外一种方式,通过AndroidBitmapInfo的大小获取宽高
    AndroidBitmapInfo info;

    // 获取Bitmap的信息,传给AndroidBitmapInfo
    AndroidBitmap_getInfo(env, bitmap, &info);

    // 定义一个数组
    int *pixels = NULL;

    // 将bitmap传成一个二维数组,并锁住bitmap  void **是一个二维数组
    AndroidBitmap_lockPixels(env, bitmap, reinterpret_cast<void **>(&pixels));

    // 绘制像素到bitmap
    int delay = drawFrame(gifFileType, info, pixels, false);

    // 解锁操作
    AndroidBitmap_unlockPixels(env, bitmap);

    // 得到当前的GifBean
    GifBean *gifBean = static_cast<GifBean *>(gifFileType->UserData);

    // 下一帧++
    gifBean->current_frame++;

    // 从头开始
    if (gifBean->current_frame > gifBean->total_frame - 1) {
        gifBean->current_frame = 0;
    }
    return delay;

}
