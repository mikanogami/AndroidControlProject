package com.example.androidcontrol.ui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.example.androidcontrol.model.AppStateViewModel.ON_CLICK;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_NOT_READY;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_ENABLED;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_RUNNING;
import static com.example.androidcontrol.model.AppStateViewModel.SERVICE_WAITING;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.androidcontrol.R;
import com.example.androidcontrol.databinding.FragmentMainBinding;
import com.example.androidcontrol.model.AppStateViewModel;

public class MainFragment extends Fragment {

    FragmentMainBinding binding;
    ActionBar actionBar;
    AppStateViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater, container, false);
        startServiceDisplay();

        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.main_page_title);

        actionBar.setDisplayHomeAsUpEnabled(false);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.settings:
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.open_settings);
                        menuHost.removeMenuProvider(this);
                        return true;
                    case R.id.instructions:
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.open_instructions);
                        menuHost.removeMenuProvider(this);
                        return true;
                    default:
                        return false;
                }
            }
        });

        /*
        viewModel = new ViewModelProvider(requireActivity()).get(AppStateViewModel.class);
        viewModel.getAppState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                switch (integer) {
                    case SERVICE_NOT_READY:
                        binding.appStateButton.setEnabled(false);
                        break;
                    case SERVICE_ENABLED:
                        binding.appStateButton.setEnabled(true);
                        binding.appStateButton.clearColorFilter();
                        binding.appStateButton.getForeground().setTint(getResources().getColor(R.color.state_ready, getActivity().getTheme()));
                        break;
                    case SERVICE_WAITING:
                        binding.appStateButton.setEnabled(true);
                        binding.appStateButton.clearColorFilter();
                        binding.appStateButton.getForeground().setTint(getResources().getColor(R.color.state_waiting, getActivity().getTheme()));
                        break;
                    case SERVICE_RUNNING:
                        binding.appStateButton.setEnabled(true);
                        binding.appStateButton.clearColorFilter();
                        binding.appStateButton.getForeground().setTint(getResources().getColor(R.color.state_running, getActivity().getTheme()));
                }
            }
        });

        binding.appStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("click?", String.valueOf(viewModel.getCurrentAppState()));
                switch (viewModel.getCurrentAppState()) {
                    case SERVICE_ENABLED:
                        viewModel.setAppState(SERVICE_WAITING);
                        break;
                    case SERVICE_WAITING:
                    case SERVICE_RUNNING:
                        viewModel.setAppState(SERVICE_ENABLED);
                }
            }
        });

        binding.startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceRunningDisplay();
                viewModel.setStartButtonState(ON_CLICK);
            }
        });
        binding.pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                servicePausedDisplay();
                viewModel.setPauseButtonState(ON_CLICK);
            }
        });
        binding.resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceRunningDisplay();
                viewModel.setResumeButtonState(ON_CLICK);
            }
        });
        binding.stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startServiceDisplay();
                viewModel.setStopButtonState(ON_CLICK);
            }
        });



         */
        return binding.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        binding.testCursor.setVisibility(GONE);
    }
}
