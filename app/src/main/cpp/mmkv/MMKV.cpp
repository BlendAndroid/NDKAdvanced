//
// Created by Administrator on 2021/3/13.
//

#include <sys/stat.h>
#include <asm/fcntl.h>
#include <fcntl.h>
#include <android/log.h>
#include "MMKV.h"

// 获取页的大小
int32_t DEFAULT_MMAP_SIZE = getpagesize();

void MMKV::initializeMMKV(const char *path) {
    // 赋值全局文件夹目录
    g_rootDir = path;
    // 创建文件夹
    mkdir(g_rootDir.c_str(), 0777);
}

MMKV *MMKV::defaultMMKV() {
    MMKV *kv = new MMKV(DEFAULT_MMAP_ID);
    return kv;
}

MMKV::MMKV(const char *mmapID) {
    m_path = g_rootDir + "/" + mmapID;
    //差生映射
    loadFromFile();
}

/**
 * 从物理内存中读取数据，并保存在map中
 */
void MMKV::loadFromFile() {
    // 打开一个文件并返回一个文件描述符
    // O_RDWR：以读写模式打开文件。相应的文件必须存在，否则打开操作会失败。
    // O_CREAT：如果指定的文件不存在，则创建一个新的文件。如果文件已经存在，则不执行任何操作。
    // S_IRWXU 00700权限，代表该文件所有者拥有读，写和执行操作的权限
    m_fd = open(m_path.c_str(), O_RDWR | O_CREAT, S_IRWXU);

    // fstat函数是C++中的一个文件状态查询函数，用于获取文件的详细信息。
    struct stat st = {0};
    // d是文件描述符，用于指定要查询的文件；buf是一个指向struct stat结构的指针，用于存储查询结果。
    if (fstat(m_fd, &st) != -1) {
        // 获取文件的大小
        m_size = st.st_size;
    }

    // 如果小于4k，或者不是4k的整数倍
    if (m_size < DEFAULT_MMAP_SIZE || (m_size % DEFAULT_MMAP_SIZE != 0)) {
        int32_t oldSize = m_size;
        // 新的4k整数倍
        m_size = ((m_size / DEFAULT_MMAP_SIZE) + 1) * DEFAULT_MMAP_SIZE;
        // 重新修改文件大小
        if (ftruncate(m_fd, m_size) != 0) {
            m_size = st.st_size;
        }
        //如果文件大小被增加了， 让增加这些大小的内容变成空, 默认都是0
        zeroFillFile(m_fd, oldSize, m_size - oldSize);
    }

    // 该函数通过指定的文件描述符 `fd` 和偏移量 `offset` 将文件映射到进程的虚拟地址空间中的地址 `addr` 处，映射的大小为 `length` 字节。以下是参数的详细说明：
    //- `addr`：映射的起始地址，通常设为0，表示由系统自动选择合适的地址。
    //- `length`：映射的大小，以字节为单位。
    //- `prot`：映射区域的保护方式，指定内存区域的读写权限。
    //- `flags`：控制映射对象的类型和映射方式，可以是以下其中一个或多个标志的组合：`MAP_SHARED`、`MAP_PRIVATE`、`MAP_FIXED`等。
    //- `fd`：要映射文件的文件描述符。
    //- `offset`：文件中的偏移量，用于指定从文件的哪个位置开始映射。
    //成功调用mmap函数将返回映射区的起始地址，失败则返回MAP_FAILED（-1），并设置相应的错误码。需要注意的是，该函数只能用于普通文件，而不能用于目录或设备文件。
    m_ptr = static_cast<int8_t *>(mmap(0, m_size, PROT_READ | PROT_WRITE, MAP_SHARED, m_fd, 0));
    // 文件头4个字节写了数据有效区长度
    memcpy(&m_actualSize, m_ptr, 4);
    __android_log_print(ANDROID_LOG_VERBOSE, "BlendAndroid", "m_actualSize=%d ", m_actualSize);

    // 有数据
    if (m_actualSize > 0) {
        // 从第4个字节开始,读取数据,保存在ProtoBuf中
        ProtoBuf inputBuffer(m_ptr + 4, m_actualSize);
        // 清空hashmap
        m_dic.clear();
        // 将文件内容解析为map
        while (!inputBuffer.isAtEnd()) {
            // 开始解析
            std::string key = inputBuffer.readString();
            __android_log_print(ANDROID_LOG_VERBOSE, "BlendAndroid", "key=%s ", key.c_str());
            // 物理内存的数据放到map中
            if (key.length() > 0) {
                // value的长度和数据
                ProtoBuf *value = inputBuffer.readData();
                //数据有效则保存，否则删除key，因为我们是append的方式写入的
                if (value && value->length() > 0) {
                    m_dic.emplace(key, value);
                }
            }
        }
    }

    // 为了后续动态扩容，需要保存一份原始数据
    m_output = new ProtoBuf(m_ptr + 4 + m_actualSize, m_size - 4 - m_actualSize);
}

/**
 * 将文件填充为0
 * @param fd 文件描述符
 * @param startPos 开始位置
 * @param size 大小
 */
void MMKV::zeroFillFile(int fd, int32_t startPos, int32_t size) {
    // 一个用于在文件中设置文件偏移量的函数
    // 参数说明：
    //- `fd`：文件描述符，表示要操作的文件。
    //- `offset`：偏移量，表示要设置的文件偏移量。
    //- `whence`：起始位置，表示相对于起始位置的偏移量，可以是`SEEK_SET`、`SEEK_CUR`或`SEEK_END`。
    //
    //`lseek`函数返回值为设置后的文件偏移量，如果出错，则返回-1，并设置errno变量来指示错误类型。
    //
    //常用的`whence`参数取值：
    //- `SEEK_SET`：从文件开头开始计算偏移量。
    //- `SEEK_CUR`：从当前位置开始计算偏移量。
    //- `SEEK_END`：从文件末尾开始计算偏移量。
    if (lseek(fd, startPos, SEEK_SET) < 0) {
        return;
    }
    // 设置一个页的数据大小
    static const char zeros[4096] = {0};

    while (size >= sizeof(zeros)) {
        if (write(fd, zeros, sizeof(zeros)) < 0) {
            return;
        }
        size -= sizeof(zeros);
    }
    if (size > 0) {
        if (write(fd, zeros, size) < 0) {
            return;
        }
    }
}

/**
 * 写入int类型数据
 *
 * @param key string，注意是取地址
 * @param value 值
 */
void MMKV::putInt(const std::string &key, int32_t value) {
    // 计算value的长度
    int32_t size = ProtoBuf::computeInt32Size(value);
    ProtoBuf *buf = new ProtoBuf(size);
    buf->writeRawInt(value);
    // 将值暂时放入hashmap中
    m_dic.emplace(key, buf);
    //将数据追加到物理内存
    appendDataWithKey(key, buf);
}

void MMKV::appendDataWithKey(std::string key, ProtoBuf *value) {
    // 计算待写入的数据大小
    int32_t itemSize = ProtoBuf::computeItemSize(key, value);
    // 如果空间不够，需要扩容
    if (itemSize > m_output->spaceLeft()) {
        // 先计算hashmap大小
        int32_t needSize = ProtoBuf::computeMapSize(m_dic);
        // 实际数据 = hashmap大小 + 表示数据长度的4个字节
        needSize += 4;

        // 计算每个item的平均长度
        int32_t avgItemSize = needSize / std::max<int32_t>(1, m_dic.size());
        // 计算hashmap将来可能增加的大小，第一次直接扩容到8
        int32_t futureUsage = avgItemSize * std::max<int32_t>(8, (m_dic.size() + 1) / 2);
        // 需要的数据大小 + hashmap的大小> 文件可写长度
        if (needSize + futureUsage >= m_size) {
            //为了防止将来使用大小不够导致频繁重写，扩充一倍
            int32_t oldSize = m_size;
            do {
                //扩充一倍
                m_size *= 2;
            } while (needSize + futureUsage >= m_size); //如果在需要的与将来可能增加的加起来比扩容后还要大，继续扩容
            // 重新设定文件大小
            ftruncate(m_fd, m_size);
            // 将oldSize后面的数据填充为0
            zeroFillFile(m_fd, oldSize, m_size - oldSize);
            // 解除映射
            munmap(m_ptr, oldSize);
            // 重新映射
            m_ptr = (int8_t *) mmap(m_ptr, m_size, PROT_READ | PROT_WRITE, MAP_SHARED, m_fd, 0);
        }

        // 扩容之后,就开始全量写入
        // 先写入数据大小 总长度
        m_actualSize = needSize - 4;
        memcpy(m_ptr, &m_actualSize, 4);

        __android_log_print(ANDROID_LOG_VERBOSE, "BlendAndroid", "extending  full write");

        // 先删除原始数据
        delete m_output;
        // 重新初始化全局缓存
        m_output = new ProtoBuf(m_ptr + 4, m_size - 4);
        // 开始遍历
        auto iter = m_dic.begin();
        for (; iter != m_dic.end(); iter++) {
            // 获取key
            auto k = iter->first;
            // 获取value
            auto v = iter->second;
            // 写入物理内存
            m_output->writeString(k);
            m_output->writeData(v);
        }
    } else {
        // 足够，直接append加入
        // 写入4个字节总长度
        m_actualSize += itemSize;
        memcpy(m_ptr, &m_actualSize, 4);

        //写入key
        m_output->writeString(key);
        //写入value
        m_output->writeData(value);
    }
}

int32_t MMKV::getInt(std::string key, int32_t defaultValue) {
    // 找到hashmap的内容，因为hashmap就是去除的
    auto itr = m_dic.find(key);
    // 如果没有到文件末尾
    if (itr != m_dic.end()) {
        // 获取hashmap的值
        ProtoBuf *buf = itr->second;
        int32_t returnValue = buf->readInt();
        // 多次读取，将position还原为0
        buf->restore();
        return returnValue;
    }
    return defaultValue;
}
