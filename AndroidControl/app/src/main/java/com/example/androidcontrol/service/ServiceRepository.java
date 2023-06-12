package com.example.androidcontrol.service;

import static com.example.androidcontrol.utils.MyConstants.FOL_CLIENT_KEY;
import static com.example.androidcontrol.utils.MyConstants.START_SIGNAL;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.androidcontrol.utils.Utils;
import com.example.androidcontrol.webrtc.RTCClient;
import com.example.androidcontrol.webrtc.SocketClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoTrack;

import java.util.Arrays;

public class ServiceRepository implements SocketClient.SocketListener, RTCClient.RTCListener {
    private static final String TAG = "ServiceRepository";
    private static final String IceSeperatorChar = "|";
    public VideoRenderListener videoRenderListener;
    Context context;
    String clientKey;
    private SocketClient socketClient;
    public RTCClient rtcClient;

    public ServiceRepository(Context context, String clientKey) {
        this.context = context;
        this.clientKey = clientKey;
        socketClient = new SocketClient(clientKey);
        rtcClient = new RTCClient(context);

        socketClient.listener = this;
        rtcClient.rtcListener = this;
    }

    public void onUnbind() {
        if (socketClient.websocket != null) {
            socketClient.websocket.close();
        }
        rtcClient.mediaStream.dispose();
    }

    public void setMProjectionIntent(Intent mProjectionIntent) {
        rtcClient.mProjectionIntent = mProjectionIntent;
    }

    public void start() {
        rtcClient.initializePeerConnectionFactory();
        rtcClient.initializePeerConnections();
        socketClient.connectToSignallingServer();
        if (clientKey.equals(FOL_CLIENT_KEY)) {
            rtcClient.createVideoTrackFromCameraAndShowIt();
            rtcClient.startStreamingVideo();
            rtcClient.createControlDataChannel();
        }
    }

    @Override
    public void handleOnNewMessage(String message) throws JSONException {
        if (message.equals(START_SIGNAL)) {
            socketClient.enableDoEncrypt();
            handleStartSignal();
            return;
        } else {
            JSONObject msgJson = new JSONObject(message);
            int messageType = msgJson.getInt("MessageType");
            String messageData = msgJson.getString("Data");

            switch (messageType) {
                case 1:
                    Log.d(TAG, "handleOfferMessage: received offer, sending answer " + message);
                    handleOfferMessage(messageData);
                    break;
                case 2:
                    Log.d(TAG, "handleAnswerMessage: received answer " + message);
                    handleAnswerMessage(messageData);
                    break;
                case 3:
                    Log.d(TAG, "handleIceCandidateMessage: receiving candidate " + message);
                    String parts[] = messageData.split("\\|");
                    String sdp = parts[0];
                    int sdpMLineIndex = Integer.parseInt(parts[1]);
                    String sdpMid = parts[2];
                    handleIceCandidateMessage(sdp, sdpMLineIndex, sdpMid);
                    break;
                default:
                    handleDefaultMessage(message);
            }
        }
    }

    public void handleStartSignal() {
        Log.d(TAG, "handleStartSignal: follower initiates the WebRTC signaling");
        if (clientKey.equals(FOL_CLIENT_KEY)) {
            rtcClient.handleStartSignal();
        }
    }
    public void handleOfferMessage(String sdpContent) {
        rtcClient.handleOfferMessage(sdpContent);
    }
    public void handleAnswerMessage(String sdpContent) {
        rtcClient.handleAnswerMessage(sdpContent);
    }
    public void handleIceCandidateMessage(String sdp, int sdpMLineIndex, String sdpMid) {
        rtcClient.handleIceCandidateMessage(sdp, sdpMLineIndex, sdpMid);
    }

    public void handleDefaultMessage(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void sendSdpToSocket(String sdp, int type) {
        sendToSocket(type, sdp);
    }

    @Override

    public void sendCandidateToSocket(String sdp, int sdpMLineIndex, String sdpMid) {
        String content = sdp
                + IceSeperatorChar + String.valueOf(sdpMLineIndex)
                + IceSeperatorChar + sdpMid;
        sendToSocket(3, content);
    }

    @Override
    public void renderControlEvent(byte[] eventBytes) {

        Intent intent = new Intent(context, ControlService.class);
        intent.putExtra("event", eventBytes);
        Log.d(TAG, String.valueOf(Utils.bytesToFloat(eventBytes)) + " "
            + String.valueOf(Utils.bytesToFloat(Arrays.copyOfRange(eventBytes, 4, 8))));

        context.startService(intent);
    }

    public void sendToSocket(int type, String content) {
        JSONObject message = new JSONObject();

        try {
            message.put("MessageType", type);
            message.put("Data", content);
            message.put("IceDataSeparator", IceSeperatorChar);
            socketClient.sendMessage(String.valueOf(message));
            Log.d(TAG, "onIceCandidate: sending candidate " + message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void renderLocalVideoTrack(VideoTrack vTrack) {
        if (videoRenderListener != null) {
            videoRenderListener.renderLocalVideoTrack(vTrack);

        }
    }
    @Override
    public void renderRemoteVideoTrack(VideoTrack vTrack) {
        if (videoRenderListener != null) {
            videoRenderListener.renderRemoteVideoTrack(vTrack);
        }
    }

    public interface VideoRenderListener {
        void renderLocalVideoTrack(VideoTrack vTrack);
        void renderRemoteVideoTrack(VideoTrack vTrack);
    }
}
