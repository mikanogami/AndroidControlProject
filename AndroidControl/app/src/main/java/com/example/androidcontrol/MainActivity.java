package com.example.androidcontrol;

import static com.example.androidcontrol.utils.AppState.AWAIT_LAUNCH_PERMISSIONS;
import static com.example.androidcontrol.utils.AppState.AWAIT_SERVICE_START;
import static com.example.androidcontrol.utils.AppState.SERVICE_BOUND_AWAIT_PEER;
import static com.example.androidcontrol.utils.AppState.LAUNCH_PERMISSIONS;
import static com.example.androidcontrol.utils.AppState.SERVICE_RUNNING;
import static com.example.androidcontrol.utils.AppState.SERVICE_READY;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_WIDTH;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.androidcontrol.databinding.ActivityMainBinding;
import com.example.androidcontrol.databinding.BubbleLayoutBinding;
import com.example.androidcontrol.service.FollowerService;
import com.example.androidcontrol.ui.BubbleHandler;
import com.example.androidcontrol.utils.AppState;
import com.example.androidcontrol.utils.UtilsPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    public static final String BUBBLE_CLICK = "on-bubble-click";
    public static final String ON_PEER_CONN = "on-peer-connect";
    public static final String ON_PEER_DISCONN = "on-peer-disconnect";
    private Intent serviceIntent;

    private Intent mProjectionIntent;
    private Boolean mIsBound;
    private FollowerService mBoundService;
    public static Window mWindow;
    public BubbleLayoutBinding serviceBubbleBinding;
    public WindowManager.LayoutParams mBubbleLayoutParams;

    ActivityResultLauncher<Intent> mProjPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "screen capture request accepted");
                            mProjectionIntent = (Intent) result.getData();
                            onMediaProjectionPermissionGranted();
                        } else {
                            Log.d(TAG, "screen capture request denied");
                            finish();
                        }
                    }
            );

    ActivityResultLauncher<Intent> appPermissionsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        launchAppPermissions();
                    }
            );

    ActivityResultLauncher<Intent> accessibilityPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        launchAccessibilityPermissions();
                    }
            );

    ActivityResultLauncher<Intent> overlaysPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        launchDrawOverlayPermission();
                    }
            );

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            FollowerService.FollowerBinder binder = (FollowerService.FollowerBinder) service;
            mBoundService = binder.getService();
            mIsBound = true;

            binder.getPeerStatus().observe(MainActivity.this, peerStatus -> {
                Log.d("peerConnectionStatus", "onChanged");
                if (peerStatus.equals(ON_PEER_CONN)) {
                    appState.onPeerConnect();
                } else if (peerStatus.equals(ON_PEER_DISCONN)) {
                    appState.onPeerDisconnect();
                }
            });
            Log.d(TAG, "Follower service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            mIsBound = false;
            destroyServiceBubble();
            Log.d(TAG, "Follower service disconnected");
        }
    };

    private static AppState appState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        appState = new ViewModelProvider(this).get(AppState.class);
        appState.getAppState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer data) {
                // update ui.
                Log.d("onAppStateChange", String.valueOf(data));
                updateAppStateButtonUI(data);
                updateBubbleButtonUI(data);
                switch (data) {
                    case AWAIT_LAUNCH_PERMISSIONS:
                        break;
                    case LAUNCH_PERMISSIONS:
                        launchAppPermissions();
                    case AWAIT_SERVICE_START:
                        onAwaitServiceStart();
                        break;
                    case SERVICE_BOUND_AWAIT_PEER:
                        startAndBindService();
                        break;
                    case SERVICE_READY:
                        onServiceReady();
                        break;
                    case SERVICE_RUNNING:
                        onServiceRunning();
                        break;
                }
            }
        });

        // app initial state based on app permissions
        setStateFromPermissions();


        binding.appStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("click?", "click.");
                appState.onMainButtonClick();
            }
        });


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        mWindow = getWindow();
        WindowCompat.setDecorFitsSystemWindows(mWindow, false);

        VIDEO_PIXELS_HEIGHT = displayMetrics.heightPixels;
        VIDEO_PIXELS_WIDTH = displayMetrics.widthPixels;
        mIsBound = false;


    }

    private void unbindFromService() {
        destroyServiceBubble();
        mIsBound = false;
        this.unbindService(mConnection);
    }

    private void onServiceReady() {
        if (mIsBound) {
            //mBoundService.onServiceReady();
        }
    }

    private void onAwaitServiceStart() {
        // double check we have our permissions
        //setStateFromPermissions();

        if (mIsBound) {
            unbindFromService();
        }
    }

    private void setStateFromPermissions() {
        if (hasAppPermissions()) {
            appState.onPermissionsGranted();
        } else {
            appState.onPermissionsNotGranted();
        }
    }

    private void onServiceRunning() {
        if (mIsBound) {
            //mBoundService.onServiceRunning();
        }
    }

    private boolean hasAppPermissions() {
        return hasDrawOverlayPermission() && hasAccessibilityPermission();
    }

    private boolean hasDrawOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private boolean hasAccessibilityPermission() {
        return UtilsPermissions.isAccessibilityPermissionGranted(this);
    }

    private void launchAppPermissions() {
        launchAccessibilityPermissions();
        launchDrawOverlayPermission();
        appState.onPermissionsGranted();
    }


    private void launchAccessibilityPermissions() {
        if (!hasAccessibilityPermission()) {
            launchPermissionRequest(
                    R.string.accessibility_hint,
                    Settings.ACTION_ACCESSIBILITY_SETTINGS,
                    null
            );
            /*
            new AlertDialog.Builder(this)
                    .setMessage(R.string.accessibility_hint)
                    .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();

             */
        }

    }

    private void launchDrawOverlayPermission() {
        if (!hasDrawOverlayPermission()) {
            launchPermissionRequest(R.string.overlays_hint, Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));

            /*
            new AlertDialog.Builder(this)
                    .setMessage(R.string.overlays_hint)
                    .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();

             */


        }
    }

    private void launchPermissionRequest(int messageId, String action, Uri uri) {
        new AlertDialog.Builder(this)
                .setMessage(messageId)
                .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(action, uri);
                        startActivity(intent);
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }




    /*
    private void checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

     */

    private void updateBubbleButtonUI(@NonNull Integer currentAppState) {
        switch (currentAppState) {
            case AWAIT_LAUNCH_PERMISSIONS:
            case LAUNCH_PERMISSIONS:
            case AWAIT_SERVICE_START:
                break;
            case SERVICE_BOUND_AWAIT_PEER:
                if (mProjectionIntent != null) {
                    if (serviceBubbleBinding == null) {
                        createServiceBubble();
                    }
                    serviceBubbleBinding.bubble1.clearColorFilter();
                    serviceBubbleBinding.getRoot().setEnabled(false);
                    serviceBubbleBinding.getRoot().setVisibility(View.VISIBLE);
                    serviceBubbleBinding.bubble1.setImageIcon(Icon.createWithResource(this, R.drawable.do_not_disturb_24));
                }
                break;
            case SERVICE_READY:
                serviceBubbleBinding.bubble1.clearColorFilter();
                serviceBubbleBinding.getRoot().setEnabled(true);
                serviceBubbleBinding.getRoot().setVisibility(View.VISIBLE);
                serviceBubbleBinding.bubble1.setImageIcon(Icon.createWithResource(this, R.drawable.play_arrow_24));
                break;
            case SERVICE_RUNNING:
                serviceBubbleBinding.bubble1.clearColorFilter();
                serviceBubbleBinding.getRoot().setEnabled(true);
                serviceBubbleBinding.getRoot().setVisibility(View.VISIBLE);
                serviceBubbleBinding.bubble1.setImageIcon(Icon.createWithResource(this, R.drawable.pause_24));
                break;
        }

    }

    private void updateAppStateButtonUI(@NonNull Integer currentAppState) {
        binding.appStateButton.setEnabled(true);
        binding.appStateButton.clearColorFilter();
        // sets default tint color
        int tintColor = getResources().getColor(R.color.white, getTheme());

        switch (currentAppState) {
            case AWAIT_LAUNCH_PERMISSIONS:
                tintColor = getResources().getColor(R.color.bubble_button_background, getTheme());
                break;
            case LAUNCH_PERMISSIONS:
                binding.appStateButton.setEnabled(false);
                tintColor = getResources().getColor(R.color.white, getTheme());
                break;
            case AWAIT_SERVICE_START:
                tintColor = getResources().getColor(R.color.white, getTheme());
                break;
            case SERVICE_BOUND_AWAIT_PEER:
                if (mProjectionIntent != null) {
                    tintColor = getResources().getColor(R.color.main_button_await_peer, getTheme());
                }
                break;
            case SERVICE_READY:
            case SERVICE_RUNNING:
                tintColor = getResources().getColor(R.color.main_button_peer_connected, getTheme());
                break;
        }
        binding.appStateButton.getForeground().setTint(tintColor);
    }

    private void startAndBindService() {
        if (mProjectionIntent == null) {
            MediaProjectionManager mProjectionManager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mProjPermissionLauncher.launch(mProjectionManager.createScreenCaptureIntent());
        } else {
            onMediaProjectionPermissionGranted();
        }
    }

    private void onMediaProjectionPermissionGranted() {
        serviceIntent = new Intent(this, FollowerService.class);
        serviceIntent.putExtra(M_PROJ_INTENT, mProjectionIntent);

        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);

        updateAppStateButtonUI(appState.getCurrentAppState());
        updateBubbleButtonUI(appState.getCurrentAppState());
    }



    private void createServiceBubble() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        serviceBubbleBinding = BubbleLayoutBinding.inflate(layoutInflater);
        serviceBubbleBinding.setHandler(new BubbleHandler(this.appState));
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


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mIsBound != null) {
            if (mIsBound) {
                unbindService(mConnection);
                mIsBound = false;
            }
        }
        super.onDestroy();
    }

    /*

    private void updateAppStateFromPeerStatus(Integer appState) {
        updateAppStateButtonUI();
        currentAppState = appState;
    }

    private void updateAppStateFromClick() {
        switch (currentAppState) {
            case SERVICE_BOUND_AWAIT_PEER:
                if (mIsBound) {
                    updateAppStateButtonUI(SERVICE_RUNNING);
                    resumeService();
                } else {
                    // UI and appState will updated if permissions are granted
                    startService();
                }
                break;
            case SERVICE_READY:
            case SERVICE_RUNNING:
                updateAppStateButtonUI(SERVICE_BOUND_AWAIT_PEER);
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

     */


}