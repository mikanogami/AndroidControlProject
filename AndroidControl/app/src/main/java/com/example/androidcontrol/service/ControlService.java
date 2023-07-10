package com.example.androidcontrol.service;

import static com.example.androidcontrol.utils.MyConstants.APP_SCREEN_PIXELS_WIDTH;
import static com.example.androidcontrol.utils.MyConstants.FULL_SCREEN_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.Utils.twoBytesToInt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Service;
import android.content.Intent;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.androidcontrol.utils.UtilsControl;

import java.util.Arrays;

public class ControlService extends AccessibilityService {

    private static final String TAG = "ControlService";
    public static final float setSigFigs = 1000.f;
    public static final int numBytesPerVal = 2;

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
        GestureDescription.StrokeDescription clickStroke = bytesToStrokeDescription(eventBytes);

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

    public static GestureDescription.StrokeDescription bytesToStrokeDescription(byte[] bytes) {

        long duration = twoBytesToInt(Arrays.copyOfRange(bytes, 4 * numBytesPerVal, 5 * numBytesPerVal));

        float[] mappedCoords = new float[4];
        float normVal;
        for(int i = 0; i < 4; i++) {
            normVal = twoBytesToInt(Arrays.copyOfRange(bytes, numBytesPerVal * i, numBytesPerVal * (i+1))) / setSigFigs;

            // x-coords are at even indices (0 and 2), y-coords are at odd indices (1 and 3)
            if(i % 2 == 0)  { mappedCoords[i] = APP_SCREEN_PIXELS_WIDTH * normVal; }
            else            { mappedCoords[i] = FULL_SCREEN_PIXELS_HEIGHT * (1 - normVal); }
        }

        GestureDescription.StrokeDescription strokeDescription;
        Log.d("bytesToStrokeDescription", mappedCoords[0] + " " + mappedCoords[1]);
        Path clickPath = new Path();
        clickPath.moveTo(mappedCoords[0], mappedCoords[1]);
        clickPath.lineTo(mappedCoords[2], mappedCoords[3]);

        return new GestureDescription.StrokeDescription(clickPath, 0, duration);
    }


}

