package com.blend.ndkadvanced.socket;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PushSocketLive implements SocketLive {

    private static final String TAG = "PushSocketLive";

    private WebSocket webSocket;

    private SocketCallback socketCallback;

    public PushSocketLive(SocketCallback socketCallback) {
        this.socketCallback = socketCallback;
    }

    @Override
    public void start() {
        Log.e(TAG, "start: ");
        webSocketServer.start();
    }

    @Override
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


    @Override
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            Log.e(TAG, "webSocket sendData: ");
            webSocket.send(bytes);
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
        public void onMessage(WebSocket conn, ByteBuffer bytes) {
            super.onMessage(conn, bytes);
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            Log.i(TAG, "接收视频数据  : " + Arrays.toString(buf));
            if (socketCallback != null) {
                socketCallback.callBack(buf);
            }
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
}
