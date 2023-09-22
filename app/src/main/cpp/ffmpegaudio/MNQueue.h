//
// Created by yangw on 2018-3-6.
//

#ifndef MYMUSIC_WLQUEUE_H
#define MYMUSIC_WLQUEUE_H

#include "queue"
#include "pthread.h"
#include "AndroidLog.h"
#include "MNPlaystatus.h"

extern "C"
{
#include "libavcodec/avcodec.h"
};


class MNQueue {

public:
    std::queue<AVPacket *> queuePacket;
    pthread_mutex_t mutexPacket;
    pthread_cond_t condPacket;
    MNPlaystatus *playstatus = NULL;

public:

    MNQueue(MNPlaystatus *playstatus);

    ~MNQueue();

    int putAvpacket(AVPacket *packet);

    int getAvpacket(AVPacket *packet);

    int getQueueSize();

    void clearAvpacket();


};


#endif //MYMUSIC_WLQUEUE_H
