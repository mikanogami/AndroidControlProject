package com.example.androidcontrol.service;

import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_RUNNING;
import static com.example.androidcontrol.utils.MyConstants.FOL_CLIENT_KEY;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;
import static com.example.androidcontrol.utils.MyConstants.PEER_CONNECTED;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.androidcontrol.R;
import com.example.androidcontrol.model.AppStateViewModel;


public class FollowerService extends Service implements ServiceRepository.PeerConnectionListener {

    private static final String TAG = "FollowerService";

    private ServiceRepository serviceRepo = new ServiceRepository(this, FOL_CLIENT_KEY);
    private final IBinder mBinder = new FollowerBinder();

    public class FollowerBinder extends Binder {
        public FollowerService getService() {
            return FollowerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("BubbleService", "onBind");
        createNotificationChannel();

        Intent notificationIntent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED );

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.BubbleMetadata bubbleData;
        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Log.d("Build.VERSION", "bubblable");
             bubbleData = new Notification.BubbleMetadata.Builder(pendingIntent,
                            Icon.createWithResource(this, R.mipmap.ic_launcher_round))
                    .setDesiredHeight(100)
                    .build();

            notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setContentText(intent.getStringExtra("inputExtra"))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    //.setBubbleMetadata(NotificationCompat.BubbleMetadata.fromPlatform(bubbleData))
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            Log.d("Build.VERSION", "cannot bubble");
            notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setContentText(intent.getStringExtra("inputExtra"))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .build();
        }



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

    @Override
    public boolean onUnbind(Intent intent) {
        serviceRepo.onUnbind();
        return super.onUnbind(intent);
    }

    @Override
    public void updateAppState(Integer integer) {
        sendBroadcastUpdateAppState(integer);
    }

    private void sendBroadcastUpdateAppState(Integer integer) {
        // The string "my-message" will be used to filer the intent
        Intent intent = new Intent("update-app-state");
        // Adding some data
        intent.putExtra("new-app-state", integer);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void onPauseService() {
        if (serviceRepo.rtcClient.mediaStream != null) {
            serviceRepo.rtcClient.mediaStream.audioTracks.get(0).setEnabled(false);
            serviceRepo.rtcClient.mediaStream.videoTracks.get(0).setEnabled(false);
        }

        serviceRepo.isPaused = true;
    }

    public void onResumeService() {
        if (serviceRepo.rtcClient.mediaStream != null) {
            serviceRepo.rtcClient.mediaStream.audioTracks.get(0).setEnabled(true);
            serviceRepo.rtcClient.mediaStream.videoTracks.get(0).setEnabled(true);
        }

        // Send test message to server
        /*
        if (serviceRepo.socketClient != null) {
            Log.d("checkIsPeerConnected", "success");
            sendBroadcastUpdateAppState(SERVICE_RUNNING);
            serviceRepo.socketClient.checkIsPeerConnected();
        }

         */

        serviceRepo.isPaused = false;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                serviceChannel.canBubble();
            }
            */

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
