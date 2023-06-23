package com.example.androidcontrol.service;

import static com.example.androidcontrol.MainActivity.BUBBLE_CLICK;
import static com.example.androidcontrol.MainActivity.LOCAL_BROADCAST_ACTION;
import static com.example.androidcontrol.MainActivity.PEER_CONN;
import static com.example.androidcontrol.MainActivity.PEER_DISCONN;
import static com.example.androidcontrol.utils.MyConstants.BUBBLE_SHORTCUT_ID;
import static com.example.androidcontrol.utils.MyConstants.FOL_CLIENT_KEY;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.androidcontrol.R;
import com.example.androidcontrol.databinding.BubbleLayoutBinding;
import com.example.androidcontrol.ui.BubbleHandler;


public class FollowerService extends Service implements ServiceRepository.PeerConnectionListener {

    private static final String TAG = "FollowerService";
    private static WindowManager mWindowManager;
    public BubbleLayoutBinding mBubbleLayoutBinding;
    public WindowManager.LayoutParams mBubbleLayoutParams;
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
        /*
        Intent notificationIntent = getPackageManager()
                .getLaunchIntentForPackage(getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

         */


        LayoutInflater layoutInflater = LayoutInflater.from(this);
        mBubbleLayoutBinding = BubbleLayoutBinding.inflate(layoutInflater);
        mBubbleLayoutBinding.setHandler(new BubbleHandler(this));
        mBubbleLayoutBinding.bubble1.setBackground(getDrawable(R.drawable.bubble_background));

        mBubbleLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        mBubbleLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        getWindowManager().addView(mBubbleLayoutBinding.getRoot(), mBubbleLayoutParams);
        onServiceAwaitPeer();

        /*
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
         */

        Notification notification = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setTicker("Service is Running")
                .setSmallIcon(R.drawable.ic_appstate_foreground)
                .setContentTitle("Track title")
                .setContentText("Artist - Album")
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
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


    public WindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        return mWindowManager;
    }


    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                NOTIF_CHANNEL_ID,
                BUBBLE_SHORTCUT_ID,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            serviceChannel.setAllowBubbles(true);
            serviceChannel.setBlockable(false);
        }


        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("getBubblePreference", String.valueOf(manager.getBubblePreference()));
            Log.d("canBubble", String.valueOf(serviceChannel.canBubble()));
            try {
                Log.d("notification_bubbles", String.valueOf(Settings.Secure.getInt(this.getContentResolver(), "notification_bubbles")));
            } catch (Settings.SettingNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        serviceRepo.onUnbind();
        return super.onUnbind(intent);
    }

    public void onServiceAwaitPeer() {
        mBubbleLayoutBinding.getRoot().setEnabled(false);
        mBubbleLayoutBinding.getRoot().setVisibility(View.VISIBLE);
        mBubbleLayoutBinding.bubble1.setImageIcon(Icon.createWithResource(this, R.drawable.do_not_disturb_24));
    }

    public void onServiceReady() {
        mBubbleLayoutBinding.getRoot().setEnabled(true);
        mBubbleLayoutBinding.getRoot().setVisibility(View.VISIBLE);
        mBubbleLayoutBinding.bubble1.setImageIcon(Icon.createWithResource(this, R.drawable.play_arrow_24));
    }

    public void onServiceRunning() {
        mBubbleLayoutBinding.getRoot().setEnabled(true);
        mBubbleLayoutBinding.getRoot().setVisibility(View.VISIBLE);
        mBubbleLayoutBinding.bubble1.setImageIcon(Icon.createWithResource(this, R.drawable.pause_24));
    }

    @Override
    public void broadcastPeerConnected() {
        sendLocalBroadcast(PEER_CONN);
    }
    @Override
    public void broadcastPeerDisconnected() {
        sendLocalBroadcast(PEER_DISCONN);
    }
    public void broadcastBubbleClicked() {
        sendLocalBroadcast(BUBBLE_CLICK);
    }

    public void sendLocalBroadcast(String update) {
        Intent intent = new Intent(LOCAL_BROADCAST_ACTION);
        intent.putExtra(Intent.EXTRA_TEXT, update);
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
