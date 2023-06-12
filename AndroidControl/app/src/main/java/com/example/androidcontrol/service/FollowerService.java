package com.example.androidcontrol.service;

import static com.example.androidcontrol.ui.FollowerActivity.mWindow;
import static com.example.androidcontrol.utils.MyConstants.FOL_CLIENT_KEY;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;

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

import com.example.androidcontrol.R;


public class FollowerService extends Service {

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
        WindowCompat.setDecorFitsSystemWindows(mWindow, false);
        createNotificationChannel();

        Intent notificationIntent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

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
                    .setBubbleMetadata(NotificationCompat.BubbleMetadata.fromPlatform(bubbleData))
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

        serviceRepo.setMProjectionIntent(mProjectionIntent);
        serviceRepo.start();

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceRepo.onUnbind();
        return super.onUnbind(intent);
    }


    public void onPauseService() {
        serviceRepo.rtcClient.mediaStream.audioTracks.get(0).setEnabled(false);
        serviceRepo.rtcClient.mediaStream.videoTracks.get(0).setEnabled(false);
    }

    public void onResumeService() {
        serviceRepo.rtcClient.mediaStream.audioTracks.get(0).setEnabled(true);
        serviceRepo.rtcClient.mediaStream.videoTracks.get(0).setEnabled(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                serviceChannel.canBubble();
            }
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
