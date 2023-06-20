package com.example.androidcontrol;

import static com.example.androidcontrol.utils.MyConstants.BUBBLE_SHORTCUT_ID;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.NOTIF_CHANNEL_ID;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_WIDTH;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.Manifest;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.androidcontrol.databinding.ActivityMainBinding;
import com.example.androidcontrol.service.FollowerService;
import com.example.androidcontrol.utils.UtilsPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    private Intent serviceIntent;
    private Intent mProjectionIntent;
    private Boolean mIsBound;
    private FollowerService mBoundService;
    public static Window mWindow;
    public static final int SERVICE_ENABLED = 1;
    public static final int SERVICE_WAITING = 2;
    public static final int SERVICE_RUNNING = 3;
    public static Integer currentAppState;


    ActivityResultLauncher<Intent> mProjPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "screen capture request accepted");
                            onMediaProjectionPermissionGranted(result);
                        } else {
                            Log.d(TAG, "screen capture request denied");
                            finish();
                        }
                    }
            );

    ActivityResultLauncher<Intent> accessibilityPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        checkAccessibilityPermissions();
                    }
            );

    ActivityResultLauncher<Intent> bubblePermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        checkAccessibilityPermissions();
                    }
            );

    ActivityResultLauncher<String> notificationsPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    (result) -> {
                        checkNotificationPermissions();
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        createNotificationChannel();

        // AppStateButton (which allows us to start service) is enabled once permissions are granted
        checkAccessibilityPermissions();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        mWindow = getWindow();
        WindowCompat.setDecorFitsSystemWindows(mWindow, false);

        VIDEO_PIXELS_HEIGHT = displayMetrics.heightPixels;
        VIDEO_PIXELS_WIDTH = displayMetrics.widthPixels;
        mIsBound = false;

        binding.appStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("click?", "click.");
                if (binding.appStateButton.isEnabled()) {
                    updateAppStateFromClick();
                }
            }

        });
    }

    NotificationManager manager;
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


        manager = getSystemService(NotificationManager.class);
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

    private boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return false;
            }
        }
        return true;
    }

    private void checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void updateAppStateButtonUI(Integer appState) {
        currentAppState = appState;
        binding.appStateButton.setEnabled(true);
        binding.appStateButton.clearColorFilter();
        // sets default tint color
        int tintColor = getResources().getColor(R.color.state_ready, getTheme());

        switch (appState) {
            case SERVICE_ENABLED:
                tintColor = getResources().getColor(R.color.state_ready, getTheme());
                break;
            case SERVICE_WAITING:
                tintColor = getResources().getColor(R.color.state_waiting, getTheme());
                break;
            case SERVICE_RUNNING:
                tintColor = getResources().getColor(R.color.state_running, getTheme());
        }
        binding.appStateButton.getForeground().setTint(tintColor);
    }

    private void updateAppStateFromPeerStatus(Integer appState) {
        updateAppStateButtonUI(appState);
        currentAppState = appState;
    }

    private void updateAppStateFromClick() {
        switch (currentAppState) {
            case SERVICE_ENABLED:
                if (mIsBound) {
                    updateAppStateButtonUI(SERVICE_RUNNING);
                    resumeService();
                } else {
                    // UI and appState will updated if permissions are granted
                    startService();
                }
                break;
            case SERVICE_WAITING:
            case SERVICE_RUNNING:
                updateAppStateButtonUI(SERVICE_ENABLED);
                pauseService();
                break;
        }
    }

    private void checkBubblePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (manager.getBubblePreference() == NotificationManager.BUBBLE_PREFERENCE_NONE) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                Log.d("getPackageName", getPackageName());
                bubblePermissionLauncher.launch(intent);
            }
        }
    }

    private void checkAccessibilityPermissions() {
        if (!UtilsPermissions.isAccessibilityPermissionGranted(this)) {
            binding.appStateButton.setEnabled(false);
            new AlertDialog.Builder(this)
                    .setMessage(R.string.accessibility_hint)
                    .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            accessibilityPermissionLauncher.launch(intent);
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            binding.appStateButton.setEnabled(true);
            updateAppStateButtonUI(SERVICE_ENABLED);
        }
    }


    private void startService() {
        MediaProjectionManager mProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjPermissionLauncher.launch(mProjectionManager.createScreenCaptureIntent());
    }

    private void onMediaProjectionPermissionGranted(ActivityResult result) {
        updateAppStateButtonUI(SERVICE_WAITING);

        mProjectionIntent = (Intent) result.getData();
        serviceIntent = new Intent(this, FollowerService.class);
        serviceIntent.putExtra(M_PROJ_INTENT, mProjectionIntent);

        checkNotificationPermissions();
        checkBubblePermissions();

        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("test-message"));
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBound != null) {
            if (mIsBound) {
                unbindService(mConnection);
                LocalBroadcastManager.getInstance(this)
                        .unregisterReceiver(messageReceiver);
                mIsBound = false;
            }
        }
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            Log.d("mikatest", "messagereceived");
            int appState = intent.getIntExtra("new-app-state", -1); // -1 is going to be used as the default value
            if (appState != -1) {
                updateAppStateFromPeerStatus(appState);
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            FollowerService.FollowerBinder binder = (FollowerService.FollowerBinder) service;
            mBoundService = binder.getService();
            mIsBound = true;
            Log.d(TAG, "Follower service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            mIsBound = false;
            Log.d(TAG, "Follower service disconnected");
        }
    };




    private void pauseService() {
        if (mIsBound) {
            mBoundService.onPauseService();
        }
    }

    private void resumeService() {
        if (mIsBound) {
            mBoundService.onResumeService();
        }
    }

}