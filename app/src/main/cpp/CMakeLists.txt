cmake_minimum_required(VERSION 3.10.2)

project("ndkadvanced")

add_subdirectory(rtmp)

#配置x264
file(GLOB x264_source ./x264/*.cpp)
#x264和faac头文件引入
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
#ffmpeg头文件引入
include_directories(${CMAKE_SOURCE_DIR}/ffmpeg/include)

#soundtouch头文件引入
include_directories(soundtouch/include)
include_directories(soundtouch/SoundTouch)

#util头文件引入
include_directories(util/include)
include_directories(util/JNI_UTIL)

#x264和faac静态库
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")
#ffmpeg动态库
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/ffmpeg/libs/${ANDROID_ABI}")

#配置soundtouch到SOUNDTOUCH_SRC
aux_source_directory(./soundtouch/SoundTouch SOUNDTOUCH_SRC)

aux_source_directory(./ffmpegaudio FFMPEG_AUDIO_SRC)

aux_source_directory(./util/JNI_UTIL JNI_UTIL_SRC)


# add_library(libname [SHARED | STATIC | MODULE] [EXCLUDE_FROM_ALL] [source])
# 将一组源文件 source 编译出一个库文件，并保存为 libname.so，其中有三种库文件类型，不写的话，默认为 STATIC
# SHARED: 表示动态库，可以在(Java)代码中使用 System.loadLibrary(name) 动态调用；
# STATIC: 表示静态库，集成到代码中会在编译时调用；
# MODULE: 只有在使用 dyId 的系统有效，如果不支持 dyId，则被当作 SHARED 对待；
# EXCLUDE_FROM_ALL: 表示这个库不被默认构建，除非其他组件依赖或手工构建;
add_library(
        native-lib
        SHARED
        native-lib.cpp
        gif/dgif_lib.c
        gif/gifalloc.c
        hello/hello_main.cpp
        gif/gif_main.cpp
        rtmp/rtmp_main.cpp
        ${x264_source}
        mmkv/MMKV_main.cpp
        mmkv/MMKV.cpp
        mmkv/ProtoBuf.cpp
        ffmpeg/ffmpeg_main.cpp
        ${SOUNDTOUCH_SRC}
        ${FFMPEG_AUDIO_SRC}
        ${JNI_UTIL_SRC}

)

# find_library(name1 path1 path2 ...) 将一个变量和Android NDK的某个库建立关联关系。
# 该函数的第二个参数为Android NDK中对应的库名称，而调用该方法之后，它就被和第一个参数所指定的变量关联在一起。
find_library(log-lib log)   #路径为空，应该是查找系统环境变量路径

find_library(jnigraphics-lib jnigraphics)

# target_link_libraries(target library <debug | optimized> library2…)
# 用来为 target 添加需要的链接的共享库，同样也可以用于为自己编写的共享库添加共享库链接。
# 把NDK库和我们自己的原生库native-lib进行关联，这样，我们就可以调用该NDK库中的函数了
target_link_libraries(
        native-lib
        ${jnigraphics-lib}
        ${log-lib}
        librtmp
        x264
        faac
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
        android
        OpenSLES
)

#set(变量名 变量值)

#指定 原生代码 或 so库 的头文件路径
#include_directories

#将路径列表全部放到一个变量中
#aux_source_directory

# 打印信息，放在了build_output.txt
message("---------------")
message(${ANDROID_ABI})
message(${ANDROID_NDK})
message(${ANDROID_PLATFORM})
message(${ANDROID_TOOLCHAIN})