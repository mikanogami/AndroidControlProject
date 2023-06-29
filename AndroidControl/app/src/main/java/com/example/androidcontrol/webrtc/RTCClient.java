package com.example.androidcontrol.webrtc;

import static com.example.androidcontrol.utils.MyConstants.*;
import static org.webrtc.SessionDescription.Type.ANSWER;
import static org.webrtc.SessionDescription.Type.OFFER;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.example.androidcontrol.utils.Type;
import com.example.androidcontrol.utils.Utils;

import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RTCClient {
    private static final String TAG = "RTCClient";

    private final Context context;

    public RTCClient.RTCListener rtcListener;

    PeerConnection peerConnection;
    public EglBase rootEglBase = EglBase.create();
    PeerConnectionFactory factory;
    VideoTrack localVideoTrack;
    public MediaStream mediaStream;
    private DataChannel localDataChannel;
    public static Intent mProjectionIntent;
    SurfaceTextureHelper mSurfaceTextureHelper;

    public RTCClient(Context context) {
        this.context = context;
    }

    public void handleDispose() {
        mediaStream.dispose();
        mSurfaceTextureHelper.stopListening();
        mSurfaceTextureHelper.dispose();
    }

    public void handleStartSignal() {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: ");
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                rtcListener.sendSdpToSocket(sessionDescription.description, Type.Offer.getVal());
            }
        }, sdpMediaConstraints);
    }

    public void handleOfferMessage(String sdpContent) {
        peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(
                OFFER, sdpContent));
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                rtcListener.sendSdpToSocket(sessionDescription.description, Type.Answer.getVal());
            }
        }, new MediaConstraints());
    }

    public void handleAnswerMessage(String sdpContent) {
        peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, sdpContent));
    }

    public void handleIceCandidateMessage(String sdp, int sdpMLineIndex, String sdpMid) {
        IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
        peerConnection.addIceCandidate(candidate);
    }

    public void initializePeerConnectionFactory() {

        PeerConnectionFactory.InitializationOptions initOptions = PeerConnectionFactory
                .InitializationOptions.builder(context)
                .createInitializationOptions();

        PeerConnectionFactory.initialize(initOptions);
        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),
                /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);

        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(
                rootEglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder().setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();
    }

    public void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);
    }

    public void createControlDataChannel() {
        Log.d(TAG, "createControlDataChannel: ");
        localDataChannel = peerConnection.createDataChannel(DATA_CHANNEL_NAME, new DataChannel.Init());
        localDataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {

            }

            @Override
            public void onStateChange() {

            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                Log.d(TAG, "follower-side onMessage: " + buffer.data);
                receiveMessageFromChannel(buffer.data);
            }
        });
    }

    public void sendMessageToChannel(byte[] message) {
        ByteBuffer data = Utils.bytesToByteBuffer(message);

        if (localDataChannel != null) {
            localDataChannel.send(new DataChannel.Buffer(data, false));
        }
    }

    public void receiveMessageFromChannel(ByteBuffer msgByteBuffer) {
        byte[] message = Utils.byteBufferToBytes(msgByteBuffer);
        rtcListener.renderControlEvent(message);
    }





    public void createVideoTrackFromCameraAndShowIt() {
        VideoCapturer videoCapturer = createScreenCapturer();
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
        mSurfaceTextureHelper = SurfaceTextureHelper.create(
                Thread.currentThread().getName(), rootEglBase.getEglBaseContext(), true);
        videoCapturer.initialize(mSurfaceTextureHelper, context,
                videoSource.getCapturerObserver());


        videoCapturer.startCapture(VIDEO_PIXELS_WIDTH, VIDEO_PIXELS_HEIGHT, FPS);
        localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(true);

        rtcListener.renderLocalVideoTrack(localVideoTrack);
    }

    public void startStreamingVideo() {
        mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(localVideoTrack);
        peerConnection.addStream(mediaStream);
        Log.d(TAG, "Client media added to stream and started streaming locally");
    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
        String URL = "stun:stun.l.google.com:19302";
        iceServers.add(new PeerConnection.IceServer(URL));

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: ");
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: ");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                rtcListener.sendCandidateToSocket(
                        iceCandidate.sdp,
                        iceCandidate.sdpMLineIndex,
                        iceCandidate.sdpMid
                        );
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved: ");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size());
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                remoteVideoTrack.setEnabled(true);
                // remoteVideoTrack.addRenderer(new VideoRenderer(binding.surfaceView2));

                rtcListener.renderRemoteVideoTrack(remoteVideoTrack);
                //remoteVideoTrack.addSink(clientActivity.getBinding().surfaceView2);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: ");
                localDataChannel = dataChannel;
                if (localDataChannel != null) {
                    localDataChannel.registerObserver(new DataChannel.Observer() {
                        @Override
                        public void onBufferedAmountChange(long l) {

                        }

                        @Override
                        public void onStateChange() {
                            Log.d(TAG, "onStateChange: remote data channel state: " + dataChannel.state().toString());
                        }

                        @Override
                        public void onMessage(DataChannel.Buffer buffer) {
                            Log.d(TAG, "expert-side onMessage: " + buffer.data);
                        }
                    });
                }
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }
        };

        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);
    }



    private VideoCapturer createScreenCapturer() {
        MediaProjection.Callback mediaProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d(TAG, "mediaProjectionCallback: capture stopped");
            }
        };

        VideoCapturer screenCapturer = new ScreenCapturerAndroid(
                mProjectionIntent, mediaProjectionCallback);
        Log.d("mProjectionIntent", String.valueOf(mProjectionIntent));
        Log.d("mediaProjectionCallback", String.valueOf(mediaProjectionCallback));
        Log.d("screenCapturer", String.valueOf(screenCapturer));

        return screenCapturer;
    }


    public interface RTCListener {
        void sendSdpToSocket(String sdp, int type);
        void sendCandidateToSocket(String sdp, int sdpMLineIndex, String sdpMid);
        void renderControlEvent(byte[] eventBytes);
        void renderLocalVideoTrack(VideoTrack vTrack);
        void renderRemoteVideoTrack(VideoTrack vTrack);
    }
}