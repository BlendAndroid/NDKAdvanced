package com.blend.ndkadvanced.socket;

public interface SocketLive {

    void start();

    void close();

    void sendData(byte[] bytes);

}
