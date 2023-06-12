package com.example.androidcontrol.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.androidcontrol.utils.UtilsControl;

public class ControlService extends AccessibilityService {

    private static final String TAG = "ControlService";


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        //Log.d(TAG, "onAccessibilityEvent");
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "control service started");

        byte[] eventBytes = intent.getByteArrayExtra("event");
        GestureDescription.StrokeDescription clickStroke = UtilsControl.bytesToStrokeDescription(eventBytes);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(clickStroke);
        boolean result = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d("dispatchGesture onCompleted", String.valueOf(gestureDescription));
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d("dispatchGesture onCancelled", String.valueOf(gestureDescription));
            }
        }, null);
        Log.d(TAG, "Gesture dispatched, result=" + result);

        return Service.START_NOT_STICKY;
    }

}

