package com.example.androidcontrol.service;

import static com.example.androidcontrol.service.ControlServiceRepository.EVENT_BYTES_TAG;
import static com.example.androidcontrol.service.ControlServiceRepository.IS_CONTINUED_TAG;
import static com.example.androidcontrol.service.ControlServiceRepository.WILL_CONTINUE_TAG;
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

import java.util.Arrays;

public class ControlService extends AccessibilityService {

    private static final String TAG = "ControlService";
    public static final float setSigFigs = 1000.f;
    public static final int numBytesPerVal = 2;
    public static final int DX_THRESHOLD = 50;
    public static final int DY_THRESHOLD = 50;
    public static final float DURATION_THRESHOLD = 500;
    float prevStrokeX;
    float prevStrokeY;
    boolean isContinued = false;
    long maxStrokeDuration = GestureDescription.getMaxGestureDuration() / (GestureDescription.getMaxGestureDuration() + 1);
    long currentStrokeDuration = 0;
    GestureDescription.StrokeDescription currentGesture;
    GestureDescription.Builder gestureBuilder;
    GestureDescription gestureConstraints;
    @Override
    public void onCreate() {
        Log.d("onCreate",  "service created");

    }

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

        byte[] eventBytes = intent.getByteArrayExtra(EVENT_BYTES_TAG);
        boolean willContinue = intent.getBooleanExtra(WILL_CONTINUE_TAG, false);

        long duration = twoBytesToInt(Arrays.copyOfRange(eventBytes, 4 * numBytesPerVal, 5 * numBytesPerVal));
        if (duration <= 0) {
            duration = 1;
        }
        currentStrokeDuration = duration;
        float[] eventCoords = bytesToScreenCoords(eventBytes);
        float x1 = eventCoords[0];
        float y1 = eventCoords[1];
        float x2 = eventCoords[2];
        float y2 = eventCoords[3];
        //float currentX = eventCoords[2];
        //float currentY = eventCoords[3];
        gestureBuilder = new GestureDescription.Builder();
        addStrokeToGesture(x1, y1, x2, y2);
        dispatchGesture();
        /*
        if (!isContinued) {
            gestureBuilder = new GestureDescription.Builder();
            isContinued = true;
            prevStrokeX = currentX;
            prevStrokeY = currentY;
            if (!willContinue) {
                addStrokeToGesture(eventCoords[0], eventCoords[1], currentX, currentY);
                dispatchGesture();
            } else {
                addStrokeToGesture(prevStrokeX, prevStrokeY, currentX, currentY);
            }
        } else {
            gestureConstraints = gestureBuilder.build();
            if (gestureConstraints.getStrokeCount() >= gestureConstraints.getMaxStrokeCount() - 1) {
                addStrokeToGesture(prevStrokeX, prevStrokeY, currentX, currentY);
                dispatchGesture();
            }

            if (!willContinue) {
                addStrokeToGesture(prevStrokeX, prevStrokeY, currentX, currentY);
                dispatchGesture();
            } else {
                if (isEventOutsideBounds(currentX, currentY)) {
                    addStrokeToGesture(prevStrokeX, prevStrokeY, currentX, currentY);
                }
            }
        }


         */
        return Service.START_NOT_STICKY;
    }

    private void addStrokeToGesture(float prevX, float prevY, float x, float y) {
        Path path = new Path();
        path.moveTo(prevX, prevY);
        path.lineTo(x, y);
        prevStrokeX = x;
        prevStrokeY = y;

        GestureDescription.StrokeDescription gesture = new GestureDescription.StrokeDescription(
                path, 0, 1);
        gestureBuilder.addStroke(gesture);
        currentStrokeDuration = 0;
    }

    private void dispatchGesture() {
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
        isContinued = false;
    }

    public static float[] bytesToScreenCoords(byte[] bytes) {

        float[] mappedCoords = new float[4];
        float normVal;
        for(int i = 0; i < 4; i++) {
            normVal = twoBytesToInt(Arrays.copyOfRange(bytes, numBytesPerVal * i, numBytesPerVal * (i+1))) / setSigFigs;

            // x-coords are at even indices (0 and 2), y-coords are at odd indices (1 and 3)
            if(i % 2 == 0)  { mappedCoords[i] = APP_SCREEN_PIXELS_WIDTH * normVal; }
            else            { mappedCoords[i] = FULL_SCREEN_PIXELS_HEIGHT * (1 - normVal); }
        }

        return mappedCoords;
    }


}

