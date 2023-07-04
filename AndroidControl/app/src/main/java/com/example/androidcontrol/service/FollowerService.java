package com.example.androidcontrol.service;

import static com.example.androidcontrol.MainActivity.ON_PEER_CONN;
import static com.example.androidcontrol.MainActivity.ON_PEER_DISCONN;
import static com.example.androidcontrol.MainActivity.mWindow;
import static com.example.androidcontrol.model.ServiceStateHolder.SERVICE_NOT_READY;
import static com.example.androidcontrol.model.ServiceStateHolder.SERVICE_READY;
import static com.example.androidcontrol.model.ServiceStateHolder.SERVICE_RUNNING;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;
import static com.example.androidcontrol.utils.MyConstants.SCREEN_PIXELS_WIDTH;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.WindowCompat;

import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.androidcontrol.R;
import com.example.androidcontrol.databinding.ActivityMainBinding;
import com.example.androidcontrol.databinding.BubbleLayoutBinding;
import com.example.androidcontrol.databinding.TrashLayoutBinding;
import com.example.androidcontrol.model.ServiceStateHolder;
import com.example.androidcontrol.ui.BubbleHandler;
import com.example.androidcontrol.ui.WindowLayoutBuilder;


public class FollowerService extends LifecycleService implements ServiceRepository.PeerConnectionListener {

    private static final String TAG = "FollowerService";
    private static WindowManager mWindowManager;
    public BubbleLayoutBinding serviceBubbleBinding;
    public WindowManager.LayoutParams mBubbleLayoutParams;
    public TrashLayoutBinding trashLayoutBinding;
    public WindowManager.LayoutParams mTrashLayoutParams;
    private final MutableLiveData<String> peerStatusLiveData = new MutableLiveData<String>();
    private ServiceRepository serviceRepo = new ServiceRepository(this);
    private final IBinder mBinder = new FollowerBinder();
    private static ServiceStateHolder serviceState;

    public class FollowerBinder extends Binder {
        public FollowerService getService() {
            return FollowerService.this;
        }
        public LiveData<String> getPeerStatus() { return peerStatusLiveData; }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        Log.d("BubbleService", "onBind");
        WindowCompat.setDecorFitsSystemWindows(mWindow, false);
        createNotificationChannel();
        /*
        Intent notificationIntent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

         */

        createServiceBubble();
        createTrashBarView();

        serviceState = new ServiceStateHolder();
        Observer<Integer> myObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer data) {
                Log.d("onServiceStateChanged", String.valueOf(data));
                updateBubbleButtonUI(data);
                switch (data) {
                    case SERVICE_NOT_READY:
                        onServiceNotReady();
                        break;
                    case SERVICE_READY:
                        onServiceReady();
                        break;
                    case SERVICE_RUNNING:
                        onServiceRunning();
                        break;
                }
            }
        };

        serviceState.getServiceState().observe(this, myObserver);

        Intent notificationIntent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(intent.getStringExtra("inputExtra"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                //.setBubbleMetadata(NotificationCompat.BubbleMetadata.fromPlatform(bubbleData))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        Intent mProjectionIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mProjectionIntent = (Intent) intent.getParcelableExtra(M_PROJ_INTENT, Intent.class);
        } else {
            mProjectionIntent = (Intent) intent.getParcelableExtra(M_PROJ_INTENT);
        }
        Log.d("onStartCommand: check mProjectionIntent", String.valueOf(mProjectionIntent));
        serviceRepo.peerConnectionListener = this;
        serviceRepo.setMProjectionIntent(mProjectionIntent);
        serviceRepo.start();


        return mBinder;
    }


    private void updateBubbleButtonUI(@NonNull Integer currentServiceState) {

        serviceBubbleBinding.bubble1.clearColorFilter();
        serviceBubbleBinding.getRoot().setVisibility(View.VISIBLE);
        serviceBubbleBinding.getRoot().setEnabled(true);
        Icon imageIcon = Icon.createWithResource(this, R.drawable.do_not_disturb_24);
        switch (currentServiceState) {
            case SERVICE_NOT_READY:
                break;
            case SERVICE_READY:
                imageIcon = Icon.createWithResource(this, R.drawable.play_arrow_24);
                break;
            case SERVICE_RUNNING:
                imageIcon = Icon.createWithResource(this, R.drawable.pause_24);
        }
        serviceBubbleBinding.bubble1.setImageIcon(imageIcon);
    }

    private GestureDetectorCompat mGestureDetector;
    public void createServiceBubble() {
        serviceBubbleBinding = BubbleLayoutBinding.inflate(LayoutInflater.from(this));
        mBubbleLayoutParams = WindowLayoutBuilder.buildBubbleWindowLayoutParams();
        getWindowManager().addView(serviceBubbleBinding.getRoot(), mBubbleLayoutParams);

        int iconDiameter = (int) (SCREEN_PIXELS_WIDTH / 6.0);
        Log.d("IconDiameter", String.valueOf(iconDiameter));
        serviceBubbleBinding.getRoot().getLayoutParams().width = iconDiameter;
        serviceBubbleBinding.getRoot().getLayoutParams().height = iconDiameter;
        getWindowManager().updateViewLayout(serviceBubbleBinding.getRoot(), mBubbleLayoutParams);

        mGestureDetector = new GestureDetectorCompat(this, new BubbleHandler(this));

        serviceBubbleBinding.bubble1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mGestureDetector.onTouchEvent(motionEvent)) {
                    return true;
                }
                return false;
            }
        });



    }

    private void createTrashBarView() {
        trashLayoutBinding = TrashLayoutBinding.inflate(LayoutInflater.from(this));
        mTrashLayoutParams = WindowLayoutBuilder.buildTrashWindowLayoutParams();
        getWindowManager().addView(trashLayoutBinding.getRoot(), mTrashLayoutParams);

        int iconDiameter = (int) (SCREEN_PIXELS_WIDTH / 6.0);
        trashLayoutBinding.trashIcon1.getLayoutParams().width = iconDiameter;
        trashLayoutBinding.trashIcon1.getLayoutParams().height = iconDiameter;
        trashLayoutBinding.trashBar.getLayoutParams().height = 2 * iconDiameter;
        getWindowManager().updateViewLayout(trashLayoutBinding.getRoot(), mTrashLayoutParams);
    }


    private void destroyServiceBubble() {
        getWindowManager().removeView(serviceBubbleBinding.getRoot());
        serviceBubbleBinding = null;
        mBubbleLayoutParams = null;
    }

    public WindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    public void reopenApp() {
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        destroyServiceBubble();
        serviceRepo.onUnbind();
        return super.onUnbind(intent);
    }

    public void onServiceNotReady() {

    }

    public void onServiceReady() {
        if (serviceRepo.rtcClient.mediaStream != null) {
            serviceRepo.rtcClient.mediaStream.videoTracks.get(0).setEnabled(false);
        }

        serviceRepo.isPaused = true;
    }

    public void onServiceRunning() {
        if (serviceRepo.rtcClient.mediaStream != null) {
            serviceRepo.rtcClient.mediaStream.videoTracks.get(0).setEnabled(true);
        }

        serviceRepo.isPaused = false;
    }

    public void onBubbleClick() {
        serviceState.onBubbleButtonClick();
    }

    @Override
    public void postPeerConnected() {
        Log.d("postPeerConnected", "attempt to broadcast");
        peerStatusLiveData.postValue(ON_PEER_CONN);
        serviceState.onPeerConnect();
    }

    @Override
    public void postPeerDisconnected() {
        Log.d("postPeerDisconnected", "attempt to broadcast");
        peerStatusLiveData.postValue(ON_PEER_DISCONN);
        serviceState.onPeerDisconnect();
    }

    private void notifyEndService() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.end_service_hint)
                .setPositiveButton(R.string.end_service_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopSelf();
                    }
                })
                .setCancelable(true)
                .create()
                .show();
    }

}