package com.example.androidcontrol;

import static com.example.androidcontrol.model.AppStateViewModel.ON_CLICK;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_NOT_READY;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_ENABLED;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_RUNNING;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_WAITING;
import static com.example.androidcontrol.utils.MyConstants.M_PROJ_INTENT;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_WIDTH;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Window;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.androidcontrol.databinding.ActivityMainBinding;
import com.example.androidcontrol.model.AppStateViewModel;
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


    ActivityResultLauncher<Intent> mProjActivityLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (result) -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "screen capture request accepted");
                            initServiceIntent(result);
                            startService();
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
                        checkAccessibilityPermissions();
                    }
            );

    private NavController navController;
    private AppStateViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        mWindow = getWindow();
        WindowCompat.setDecorFitsSystemWindows(mWindow, false);

        VIDEO_PIXELS_HEIGHT = displayMetrics.heightPixels;
        VIDEO_PIXELS_WIDTH = displayMetrics.widthPixels;
        mIsBound = false;



        viewModel = new ViewModelProvider(this).get(AppStateViewModel.class);
        checkAccessibilityPermissions();
        viewModel.getAppState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Log.d("onChanged", String.valueOf(integer));
                if (mIsBound) {
                    switch (integer) {
                        case SERVICE_NOT_READY:
                            break;
                        case SERVICE_ENABLED:
                            pauseService();
                            break;
                        case SERVICE_WAITING:
                            resumeService();
                            break;
                        case SERVICE_RUNNING:
                    }
                } else {
                    switch (integer) {
                        case SERVICE_NOT_READY:
                        case SERVICE_ENABLED:
                            break;
                        case SERVICE_WAITING:
                            if (mProjectionIntent != null) {
                                startService();
                            } else {
                                getMediaProjectionPermission();
                            }
                            break;
                        case SERVICE_RUNNING:
                    }
                }

            }
        });
        viewModel.getStartButtonState().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == ON_CLICK) {
                    Log.d(TAG, "startClicked");
                    if (mProjectionIntent != null) {
                        if (mIsBound) {

                        } else {
                            startService();
                        }
                    } else {
                        getMediaProjectionPermission();
                    }
                    viewModel.setStartButtonState(!ON_CLICK);
                }
            }

        });
        viewModel.getPauseButtonState().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == ON_CLICK) {
                    Log.d(TAG, "pauseClicked");
                    viewModel.setPauseButtonState(!ON_CLICK);
                }
            }
        });
        viewModel.getResumeButtonState().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == ON_CLICK) {
                    Log.d(TAG, "resumeClicked");
                    viewModel.setResumeButtonState(!ON_CLICK);
                }

            }
        });
        viewModel.getStopButtonState().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean == ON_CLICK) {
                    Log.d(TAG, "stopClicked");
                    viewModel.setStopButtonState(!ON_CLICK);
                }
            }
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        //navController.popBackStack(R.id.main_fragment, false);
        navController.navigateUp();
        return true;
    }


    private void updateViewModelState(Integer integer) {
        viewModel.setAppState(integer);
    }

    private void checkAccessibilityPermissions() {
        if (!UtilsPermissions.isAccessibilityPermissionGranted(this)) {
            updateViewModelState(SERVICE_NOT_READY);
            new AlertDialog.Builder(this)
                    .setMessage(R.string.accessibility_hint)
                    .setPositiveButton(R.string.continue_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            accessibilityActivityLauncher.launch(intent);
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            updateViewModelState(SERVICE_ENABLED);
        }
    }

    private void getMediaProjectionPermission() {
        MediaProjectionManager mProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mProjActivityLauncher.launch(mProjectionManager.createScreenCaptureIntent());
    }

    private void startService() {
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("update-app-state"));
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
            int appState = intent.getIntExtra("new-app-state", -1); // -1 is going to be used as the default value
            if (appState != -1) {
                updateViewModelState(appState);
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

    private void initServiceIntent(ActivityResult result) {
        mProjectionIntent = (Intent) result.getData();
        serviceIntent = new Intent(this, FollowerService.class);
        serviceIntent.putExtra(M_PROJ_INTENT, mProjectionIntent);
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

}