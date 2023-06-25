package com.blend.ndkadvanced.screenshare.push;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PushSocketLive {

    private static final String TAG = "PushSocketLive";

    private WebSocket webSocket;
    private int port;

    public PushSocketLive(int port) {
        this.port = port;
    }

    public void start() {
        Log.e(TAG, "start: ");
        webSocketServer.start();
    }

    public void close() {
        try {
            webSocket.close();
            webSocketServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private final WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(12001)) {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
            Log.e(TAG, "onOpen: ");
            PushSocketLive.this.webSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket webSocket, int i, String s, boolean b) {
            Log.i(TAG, "onClose: 关闭 socket ");
        }

        @Override
        public void onMessage(WebSocket webSocket, String s) {
            Log.e(TAG, "onMessage: ");
        }

        @Override
        public void onError(WebSocket webSocket, Exception e) {
            e.printStackTrace();
            Log.i(TAG, "onError:  " + e.toString());
        }

        @Override
        public void onStart() {
            Log.e(TAG, "onStart: ");
        }
    };


    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            Log.e(TAG, "webSocket sendData: ");
            webSocket.send(bytes);
        }
    }
}
