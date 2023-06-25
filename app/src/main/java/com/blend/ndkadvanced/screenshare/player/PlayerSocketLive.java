package com.blend.ndkadvanced.screenshare.player;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class PlayerSocketLive {
    private static final String TAG = "PlayerSocketLive";

    private SocketCallback socketCallback;
    private MyWebSocketClient myWebSocketClient;
    private int port;

    public PlayerSocketLive(SocketCallback socketCallback, int port) {
        this.socketCallback = socketCallback;
        this.port = port;
    }

    public void start() {
        try {
            URI url = new URI("ws://10.221.150.178:12001");
            myWebSocketClient = new MyWebSocketClient(url);
            myWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Log.e(TAG, "打开 socket  onOpen: ");
        }

        @Override
        public void onMessage(String s) {
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            Log.e(TAG, "消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e(TAG, "onClose code: " + code + " reason: " + reason + " remote: " + remote);
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onError: ");
        }
    }


    public interface SocketCallback {
        void callBack(byte[] data);
    }
}
