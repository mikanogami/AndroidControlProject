package com.example.androidcontrol.service;

import static com.example.androidcontrol.MainActivity.ON_PEER_CONN;
import static com.example.androidcontrol.MainActivity.ON_PEER_DISCONN;
import static com.example.androidcontrol.MainActivity.mWindow;
import static com.example.androidcontrol.model.ServiceStateHolder.SERVICE_NOT_READY;
import static com.example.androidcontrol.model.ServiceStateHolder.SERVICE_READY;
import static com.example.androidcontrol.model.ServiceStateHolder.SERVICE_RUNNING;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import androidx.core.view.WindowCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.androidcontrol.R;
import com.example.androidcontrol.databinding.BubbleLayoutBinding;
import com.example.androidcontrol.model.ServiceStateHolder;
import com.example.androidcontrol.ui.BubbleHandler;


public class FollowerService extends LifecycleService implements ServiceRepository.PeerConnectionListener {

    private static final String TAG = "FollowerService";
    private static WindowManager mWindowManager;
    public BubbleLayoutBinding serviceBubbleBinding;
    public WindowManager.LayoutParams mBubbleLayoutParams;
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

        //onServiceAwaitPeer();

        /*
        Person bubbleHandler = new Person.Builder()
        Person bubbleHandler = new Person.Builder()
                .setImportant(true)
                .setName(BUBBLE_SHORTCUT_ID)
                .build();


        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, "shortcut-id")
                .setLongLived(true)
                .setShortLabel(bubbleHandler.getName())
                .setIntent(new Intent(Intent.ACTION_DEFAULT))
                .setPerson(bubbleHandler)
                .build();

        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut);

        Log.d("Build.VERSION", "bubblable");
        //PendingIntent broadcast = PendingIntent.getBroadcast(this, 0, new Intent("test-message"), PendingIntent.FLAG_MUTABLE);
        //NotificationCompat.Action action = new NotificationCompat.Action(null, null, broadcast);
        PendingIntent bubbleIntent = PendingIntent.getActivity(this, 0, new Intent(this, BubbleActivity.class), PendingIntent.FLAG_IMMUTABLE);


        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setSmallIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher_round))
                .setStyle(new NotificationCompat.MessagingStyle(bubbleHandler))
                .setBubbleMetadata(new NotificationCompat.BubbleMetadata.Builder(bubbleIntent,
                        IconCompat.createWithResource(this, R.mipmap.ic_launcher_round))
                        .setDesiredHeight(100)
                        .build())
                .setShortcutId(shortcut.getId())
                .addPerson(bubbleHandler)
                .setOngoing(true)
                .build();

                        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setTicker("Service is Running")
                .setSmallIcon(R.drawable.ic_appstate_foreground)
                .setContentTitle("Track title")
                .setContentText("Artist - Album")
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
         */

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
        //peerStatusLiveData.postValue(ON_PEER_DISCONN);
        serviceRepo.peerConnectionListener = this;
        serviceRepo.setMProjectionIntent(mProjectionIntent);
        serviceRepo.start();


        return mBinder;
    }

    private void updateBubbleButtonUI(@NonNull Integer currentServiceState) {
        if (serviceBubbleBinding == null) {
            createServiceBubble();
        }

        serviceBubbleBinding.bubble1.clearColorFilter();
        serviceBubbleBinding.getRoot().setVisibility(View.VISIBLE);
        serviceBubbleBinding.getRoot().setEnabled(true);
        Icon imageIcon = Icon.createWithResource(this, R.drawable.do_not_disturb_24);
        switch (currentServiceState) {
            case SERVICE_NOT_READY:
                serviceBubbleBinding.getRoot().setEnabled(false);
                break;
            case SERVICE_READY:
                imageIcon = Icon.createWithResource(this, R.drawable.play_arrow_24);
                break;
            case SERVICE_RUNNING:
                imageIcon = Icon.createWithResource(this, R.drawable.pause_24);
        }
        serviceBubbleBinding.bubble1.setImageIcon(imageIcon);
    }

    public void createServiceBubble() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        serviceBubbleBinding = BubbleLayoutBinding.inflate(layoutInflater);
        serviceBubbleBinding.setHandler(new BubbleHandler(this));
        serviceBubbleBinding.bubble1.setBackground(getDrawable(R.drawable.bubble_background));

        mBubbleLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        mBubbleLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        getWindowManager().addView(serviceBubbleBinding.getRoot(), mBubbleLayoutParams);
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
    public void broadcastPeerConnected() {
        Log.d("broadcastPeerConnected", "attempt to broadcast");
        peerStatusLiveData.postValue(ON_PEER_CONN);
        serviceState.onPeerConnect();
    }

    @Override
    public void broadcastPeerDisconnected() {
        Log.d("broadcastPeerDisconnected", "attempt to broadcast");
        peerStatusLiveData.postValue(ON_PEER_DISCONN);
        serviceState.onPeerDisconnect();
    }



}