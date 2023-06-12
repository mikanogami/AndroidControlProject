package com.example.androidcontrol.ui;

import static android.view.View.*;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.example.androidcontrol.R;
import com.example.androidcontrol.service.FollowerService;
import com.example.androidcontrol.service.FollowerService.FollowerBinder;
import com.example.androidcontrol.databinding.ActivityFollowerBinding;
import com.example.androidcontrol.utils.UtilsPermissions;

public class FollowerActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityFollowerBinding binding;
    private Intent serviceIntent;
    private Intent mProjectionIntent;
    private Boolean mIsBound;
    private FollowerService mBoundService;
    public static Window mWindow;

    ActivityResultLauncher<Intent> activityLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "screen capture request accepted");
                            initServiceIntent(result);
                        } else {
                            Log.d(TAG, "screen capture request denied");
                            finish();
                        }
                    }
            );

    ActivityResultLauncher<Intent> accessibilityActivityLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        Log.d("result.getResultCode()", String.valueOf(result.getResultCode()));
                        Log.d("result.getData()", String.valueOf(result.getResultCode()));

                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "accessibility request accepted");
                        } else {
                            Log.d(TAG, "accessibility request denied");
                            finish();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mWindow = getWindow();
        WindowCompat.setDecorFitsSystemWindows(mWindow, false);
        binding = ActivityFollowerBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        startServiceDisplay();

        MediaProjectionManager mProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activityLauncher.launch(mProjectionManager.createScreenCaptureIntent());
        //checkPermissions();
        checkAccessibility();

        binding.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStartService();
            }
        });
        binding.pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPauseService();
            }
        });
        binding.resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResumeService();
            }
        });
        binding.stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopService();
            }
        });
    }

    private void checkPermissions() {

    }

    private void checkAccessibility() {
        if (!UtilsPermissions.isAccessibilityPermissionGranted(this)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.accessibility_hint)
                    .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(intent);

                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBound != null) {
            onStopService();
        }
    }



    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            FollowerBinder binder = (FollowerBinder) service;
            mBoundService = binder.getService();
            mIsBound = true;
            Log.d(TAG, "Follower service connected");
        }

        public void onServiceDisconnected(ComponentName className) {
            mIsBound = false;
            Log.d(TAG, "Follower service disconnected");
        }
    };

    private void initServiceIntent(ActivityResult result) {
        mProjectionIntent = (Intent) result.getData();
        serviceIntent = new Intent(this, FollowerService.class);
        serviceIntent.putExtra(M_PROJ_INTENT, mProjectionIntent);
    }

    private void onStartService() {
        //serviceRunningDisplay();
        refreshDisplay();
        Log.d("layout dimensions", String.valueOf(binding.getRoot().getWidth()) + " " + String.valueOf(binding.getRoot().getHeight()));
        binding.view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d("raw coordinates", motionEvent.getRawX() + " " + motionEvent.getRawY());
                Log.d("regular coordinates", motionEvent.getX() + " " + motionEvent.getY());
                binding.testCursor.setX(motionEvent.getX());
                binding.testCursor.setY(motionEvent.getY());
                return false;
            }
        });
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void onPauseService() {
        servicePausedDisplay();
        if (mIsBound) {
            mBoundService.onPauseService();
        }
    }

    private void onResumeService() {
        serviceRunningDisplay();
        if (mIsBound) {
            mBoundService.onResumeService();
        }
    }

    private void onStopService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void startServiceDisplay() {
        refreshDisplay();
        binding.startService.setVisibility(VISIBLE);
    }

    private void serviceRunningDisplay() {
        refreshDisplay();
        binding.serviceRunning.setVisibility(VISIBLE);
        binding.stopService.setVisibility(VISIBLE);
    }

    private void servicePausedDisplay() {
        refreshDisplay();
        binding.servicePaused.setVisibility(VISIBLE);
        binding.stopService.setVisibility(VISIBLE);
    }

    private void refreshDisplay() {
        binding.startService.setVisibility(GONE);
        binding.serviceRunning.setVisibility(GONE);
        binding.servicePaused.setVisibility(GONE);
        binding.stopService.setVisibility(GONE);
    }

}