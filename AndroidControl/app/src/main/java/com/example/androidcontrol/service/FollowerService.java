package com.example.androidcontrol.service;

import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_RUNNING;
import static com.example.androidcontrol.utils.MyConstants.BUBBLE_SHORTCUT_ID;
import static com.example.androidcontrol.utils.MyConstants.FOL_CLIENT_KEY;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;
import static com.example.androidcontrol.utils.MyConstants.PEER_CONNECTED;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.content.LocusIdCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.androidcontrol.MainActivity;
import com.example.androidcontrol.R;
import com.example.androidcontrol.ui.BubbleActivity;


public class FollowerService extends Service implements ServiceRepository.PeerConnectionListener {

    private static final String TAG = "FollowerService";
    private static WindowManager mWindowManager;
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

        /*
        Intent notificationIntent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

         */


        Person bubbleHandler = new Person.Builder()
                .setBot(true)
                .setImportant(true)
                .setName(BUBBLE_SHORTCUT_ID)
                .build();


        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, "shortcut-id")
                .setLongLived(true)
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher_round))
                .setShortLabel(bubbleHandler.getName())
                .setIntent(new Intent(Intent.ACTION_SEND))
                .setPerson(bubbleHandler)
                .build();

        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut);


        Log.d("Build.VERSION", "bubblable");
        //PendingIntent broadcast = PendingIntent.getBroadcast(this, 0, new Intent("test-message"), PendingIntent.FLAG_MUTABLE/* flags */);
        //NotificationCompat.Action action = new NotificationCompat.Action(null, null, broadcast);
        PendingIntent bubbleIntent = PendingIntent.getActivity(this, 0, new Intent(this, BubbleActivity.class), PendingIntent.FLAG_MUTABLE/* flags */);

        Log.d("bubbleIntent", String.valueOf(bubbleIntent));
        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setLocalOnly(true)
                .setStyle(new NotificationCompat.MessagingStyle(bubbleHandler))
                .setBubbleMetadata(new NotificationCompat.BubbleMetadata.Builder(bubbleIntent, IconCompat.createWithResource(this, R.mipmap.ic_launcher_round)).setDesiredHeight(100).build())
                .addPerson(bubbleHandler)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .build();


        /*else {
            Log.d("Build.VERSION", "cannot bubble");
            notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setContentText(intent.getStringExtra("inputExtra"))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        }

         */

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

    public WindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        return mWindowManager;
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
        Log.d("testBroadcastString", getPackageName() + R.string.UPDATE_PEER_STATUS);
        Intent intent = new Intent(getPackageName() + R.string.UPDATE_PEER_STATUS);
        // Adding some data
        intent.putExtra(Intent.EXTRA_TEXT, integer);
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


}
