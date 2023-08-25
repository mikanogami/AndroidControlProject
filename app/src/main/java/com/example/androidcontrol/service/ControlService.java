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
    long currentStrokeDuration;
    GestureDescription.StrokeDescription currentGesture;
    GestureDescription.Builder gestureBuilder;

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
        boolean isContinued = intent.getBooleanExtra(IS_CONTINUED_TAG, false);
        boolean willContinue = intent.getBooleanExtra(WILL_CONTINUE_TAG, false);

        long duration = twoBytesToInt(Arrays.copyOfRange(eventBytes, 4 * numBytesPerVal, 5 * numBytesPerVal));
        if (duration <= 0) {
            duration = 1;
        }

        float[] eventCoords = bytesToScreenCoords(eventBytes);
        float currentX = eventCoords[2];
        float currentY = eventCoords[3];

        if (!isContinued | gestureBuilder == null) {
            // create new gesture
            gestureBuilder = new GestureDescription.Builder();
            prevStrokeX = currentX;
            prevStrokeY = currentY;
            GestureDescription.StrokeDescription gesture = new GestureDescription.StrokeDescription(
                    buildStrokePath(currentX, currentY), 0, duration);
            gestureBuilder.addStroke(gesture);
        }

        //currentStrokeDuration += duration;

        if (!willContinue) {
            GestureDescription.StrokeDescription gesture = new GestureDescription.StrokeDescription(
                    buildStrokePath(currentX, currentY), 0, duration);
            gestureBuilder.addStroke(gesture);
            dispatchGesture();
        } else {
            if (isEventOutsideBounds(currentX, currentY)) {
                GestureDescription.StrokeDescription gesture = new GestureDescription.StrokeDescription(
                        buildStrokePath(currentX, currentY), 0, duration);
                gestureBuilder.addStroke(gesture);
            }
        }

        GestureDescription gestureDescription = gestureBuilder.build();
        if (gestureDescription.getStrokeCount() >= gestureDescription.getMaxStrokeCount() - 1) {
            GestureDescription.StrokeDescription gesture = new GestureDescription.StrokeDescription(
                    buildStrokePath(currentX, currentY), 0, duration);
            gestureBuilder.addStroke(gesture);
            dispatchGesture();
            gestureBuilder = null;
        }

        return Service.START_NOT_STICKY;
    }

    private Path buildStrokePath(float x, float y) {
        Path path = new Path();
        path.moveTo(prevStrokeX, prevStrokeY);
        path.lineTo(x, y);

        prevStrokeX = x;
        prevStrokeY = y;
        return path;
    }

    private boolean isEventOutsideBounds(float x, float y) {
        return Math.abs(prevStrokeX - x) > DX_THRESHOLD | Math.abs(prevStrokeY - y) > DY_THRESHOLD;
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
    }

    public static Path bytesToGesturePath(byte[] bytes) {

        float[] mappedCoords = new float[4];
        float normVal;
        for(int i = 0; i < 4; i++) {
            normVal = twoBytesToInt(Arrays.copyOfRange(bytes, numBytesPerVal * i, numBytesPerVal * (i+1))) / setSigFigs;

            // x-coords are at even indices (0 and 2), y-coords are at odd indices (1 and 3)
            if(i % 2 == 0)  { mappedCoords[i] = APP_SCREEN_PIXELS_WIDTH * normVal; }
            else            { mappedCoords[i] = FULL_SCREEN_PIXELS_HEIGHT * (1 - normVal); }
        }

        Log.d("bytesToStrokeDescription", mappedCoords[0] + " " + mappedCoords[1]);
        Path clickPath = new Path();
        clickPath.moveTo(mappedCoords[0], mappedCoords[1]);
        clickPath.lineTo(mappedCoords[2], mappedCoords[3]);

        return clickPath;
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
