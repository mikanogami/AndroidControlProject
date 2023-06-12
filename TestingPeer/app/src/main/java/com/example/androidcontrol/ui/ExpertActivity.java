package com.example.androidcontrol.ui;


import static com.example.androidcontrol.utils.MyConstants.*;
import static com.example.androidcontrol.utils.UtilsControl.motionEventToBytes;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;

import com.example.androidcontrol.service.ServiceRepository;
import com.example.androidcontrol.databinding.ActivityExpertBinding;

import androidx.databinding.DataBindingUtil;


import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoTrack;

import com.example.androidcontrol.R;

public class ExpertActivity extends AppCompatActivity
        implements ServiceRepository.VideoRenderListener {
    private static final String TAG = "ExpertActivity";

    SurfaceTextureHelper surfaceTextureHelper;

    private ActivityExpertBinding binding;
    private ServiceRepository serviceRepo = new ServiceRepository(this, EXP_CLIENT_KEY);
    private boolean isScreenShare;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_expert);
        ViewGroup.LayoutParams params = binding.display.getLayoutParams();
        Log.d(TAG, "width: " + params.width + ", height: " + params.height);

        isScreenShare = false;
        serviceRepo.videoRenderListener = this;
        initializeSurfaceViews();
        serviceRepo.start();
        binding.surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (isScreenShare) {
                    Log.d("touch registered on expert side", String.valueOf(event.getX()) + "  " + String.valueOf(event.getY()));
                    DISPLAY_SURFACE_WIDTH = binding.surfaceView.getWidth();
                    DISPLAY_SURFACE_HEIGHT = binding.surfaceView.getHeight();
                    sendRemoteTouch(event);
                }
                return false;
            }
        });
    }

    private void initializeSurfaceViews() {
        binding.surfaceView.init(serviceRepo.rtcClient.rootEglBase.getEglBaseContext(), null);
        binding.surfaceView.setEnableHardwareScaler(true);
        binding.surfaceView.setScalingType(SCALE_ASPECT_FIT);
        binding.surfaceView.setMirror(false);
    }

    public void sendRemoteTouch(MotionEvent event) {
        byte[] bytes = motionEventToBytes(event);
        serviceRepo.rtcClient.sendMessageToChannel(bytes);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        serviceRepo.onUnbind();
    }

    @Override
    public void renderLocalVideoTrack(VideoTrack vTrack) {

    }

    @Override
    public void renderRemoteVideoTrack(VideoTrack vTrack) {
        isScreenShare = true;
        vTrack.addSink(binding.surfaceView);
    }
}
