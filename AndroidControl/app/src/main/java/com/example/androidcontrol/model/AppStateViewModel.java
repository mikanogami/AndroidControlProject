package com.example.androidcontrol.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AppStateViewModel extends ViewModel {

    public static final int SERVICE_NOT_READY = 0;
    public static final int SERVICE_ENABLED = 1;
    public static final int SERVICE_WAITING = 2;
    public static final int SERVICE_RUNNING = 3;


    private final MutableLiveData<Integer> appState = new MutableLiveData<Integer>();
    private static Integer currentAppState = null;
    public void setAppState(Integer item) {
        appState.setValue(item);
        currentAppState = item;
    }
    public LiveData<Integer> getAppState() { return appState; }
    public Integer getCurrentAppState() { return currentAppState; }



    public static final boolean ON_CLICK = true;
    private final MutableLiveData<Boolean> startButtonState = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> pauseButtonState = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> resumeButtonState = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> stopButtonState = new MutableLiveData<Boolean>();
    private final MutableLiveData<Boolean> isPeerConnected = new MutableLiveData<Boolean>();

    public void setStartButtonState(Boolean item) { startButtonState.setValue(item); }
    public void setPauseButtonState(Boolean item) { pauseButtonState.setValue(item); }
    public void setResumeButtonState(Boolean item) { resumeButtonState.setValue(item); }
    public void setStopButtonState(Boolean item) { stopButtonState.setValue(item); }
    public void setIsPeerConnected(Boolean item) { isPeerConnected.setValue(item); }



    public LiveData<Boolean> getStartButtonState() { return startButtonState; }
    public LiveData<Boolean> getPauseButtonState() { return pauseButtonState; }
    public LiveData<Boolean> getResumeButtonState() { return resumeButtonState; }
    public LiveData<Boolean> getStopButtonState() { return stopButtonState; }
    public LiveData<Boolean> getIsPeerConnected() { return isPeerConnected; }

}
