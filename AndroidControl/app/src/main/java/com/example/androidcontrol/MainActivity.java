package com.example.androidcontrol;

import static com.example.androidcontrol.model.ActivityStateHolder.AWAIT_LAUNCH_PERMISSIONS;
import static com.example.androidcontrol.model.ActivityStateHolder.AWAIT_SERVICE_START;
import static com.example.androidcontrol.model.ActivityStateHolder.SERVICE_BOUND;
import static com.example.androidcontrol.model.ActivityStateHolder.LAUNCH_PERMISSIONS;
import static com.example.androidcontrol.utils.MyConstants.APP_SCREEN_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.APP_SCREEN_PIXELS_WIDTH;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.androidcontrol.databinding.ActivityMainBinding;
import com.example.androidcontrol.service.FollowerService;
import com.example.androidcontrol.model.ActivityStateHolder;
import com.example.androidcontrol.utils.UtilsPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    public static final String ON_PEER_CONN = "on-peer-connect";
    public static final String ON_PEER_DISCONN = "on-peer-disconnect";
    private Intent serviceIntent;

    private Intent mProjectionIntent;
    private Boolean mIsBound;
    private FollowerService mBoundService;
    public static Window mWindow;

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

    ActivityResultLauncher<Intent> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        if (result.getResultCode() != RESULT_OK) {
                            Log.d("CheckPermissions", "not accepted");
                            activityStateHolder.onPermissionsNotGranted();
                        }
                    }
            );


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            FollowerService.FollowerBinder binder = (FollowerService.FollowerBinder) service;
            mBoundService = binder.getService();
            mIsBound = true;

            binder.getNotifyEndService().observe(MainActivity.this, serviceStatus -> {
                Log.d("serviceStatus", "onChanged");
                if (serviceStatus.equals(null)) {
                    // DO NOTHING
                } else {
                    onTryUnbindService();
                }
            });

            Log.d(TAG, "Follower service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            mIsBound = false;
            Log.d(TAG, "Follower service disconnected");
        }
    };

    private static ActivityStateHolder activityStateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        DisplayMetrics appDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(appDisplayMetrics);
        APP_SCREEN_PIXELS_HEIGHT = appDisplayMetrics.heightPixels;
        APP_SCREEN_PIXELS_WIDTH = appDisplayMetrics.widthPixels;
        Log.d("MainBinding", String.valueOf(appDisplayMetrics.heightPixels));

        mWindow = getWindow();
        //wWindowCompat.setDecorFitsSystemWindows(mWindow, false);

        int buttonDiameter = (int) (APP_SCREEN_PIXELS_WIDTH / 3.0);
        binding.appStateButton.getLayoutParams().width = buttonDiameter;
        binding.appStateButton.getLayoutParams().height = buttonDiameter;

        activityStateHolder = new ViewModelProvider(this).get(ActivityStateHolder.class);
        activityStateHolder.getAppState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer data) {
                // update ui.
                Log.d("onAppStateChange", String.valueOf(data));
                updateAppStateButtonUI(data);
                switch (data) {
                    case AWAIT_LAUNCH_PERMISSIONS:
                        onAwaitLaunchPermissions();
                        break;
                    case LAUNCH_PERMISSIONS:
                        onLaunchPermissions();
                        break;
                    case AWAIT_SERVICE_START:
                        onAwaitServiceStart();
                        break;
                    case SERVICE_BOUND:
                        onServiceBound();
                        break;
                }
            }
        });

        binding.appStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("click?", "click.");
                if (mIsBound) {
                    onTryUnbindService();
                } else {
                    activityStateHolder.onMainButtonClick();
                }
            }
        });

        mIsBound = false;
    }

    private void onTryUnbindService() {
        Log.d("activityState", String.valueOf(activityStateHolder.getCurrentAppState()));

        new AlertDialog.Builder(this)
                .setMessage(R.string.end_service_hint)
                .setPositiveButton(R.string.end_service_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        mIsBound = false;
                        unbindService(mConnection);
                        activityStateHolder.onMainButtonClick();
                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        updateAppStateButtonUI(SERVICE_BOUND);
                    }
                })
                .setCancelable(false)
                .create()
                .show();
    }


    private void onAwaitLaunchPermissions() {
        if (hasAppPermissions()) {
            activityStateHolder.onPermissionsGranted();
        } else if (!activityStateHolder.getCurrentAppState().equals(AWAIT_LAUNCH_PERMISSIONS)) {
            activityStateHolder.onPermissionsNotGranted();
        }
    }

    private void onAwaitServiceStart() {

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

    private void onLaunchPermissions() {
        launchAccessibilityPermissions();
        launchDrawOverlayPermission();
        activityStateHolder.onPermissionsGranted();
    }


    private void launchAccessibilityPermissions() {
        if (!hasAccessibilityPermission()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS, null);
            intent.putExtra("message-id", R.string.accessibility_hint);
            launchPermissionRequest(intent);
        }
    }

    private void launchDrawOverlayPermission() {
        if (!hasDrawOverlayPermission()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.putExtra("message-id", R.string.overlays_hint);
            launchPermissionRequest(intent);
        }
    }

    private void launchPermissionRequest(Intent intent) {
        new AlertDialog.Builder(this)
                .setMessage(intent.getIntExtra("message-id", -1))
                .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Intent intent = new Intent(action, uri);
                        permissionLauncher.launch(intent);
                    }
                })
                .setCancelable(false)
                .create()
                .show();
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
            case SERVICE_BOUND:
                tintColor = getResources().getColor(R.color.main_button_bound, getTheme());
                break;
        }
        binding.appStateButton.getForeground().setTint(tintColor);
    }

    private void onServiceBound() {
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

        updateAppStateButtonUI(activityStateHolder.getCurrentAppState());
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
        finishAffinity();
        super.onDestroy();
    }
}