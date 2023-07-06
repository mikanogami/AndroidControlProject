package com.example.androidcontrol.utils;

import static com.example.androidcontrol.utils.MyConstants.APP_SCREEN_PIXELS_WIDTH;
import static com.example.androidcontrol.utils.MyConstants.DISPLAY_SURFACE_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.DISPLAY_SURFACE_WIDTH;
import static com.example.androidcontrol.utils.MyConstants.FULL_SCREEN_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.PROJECTED_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.PROJECTED_PIXELS_WIDTH;
import static com.example.androidcontrol.utils.Utils.intToTwoBytes;
import static com.example.androidcontrol.utils.Utils.twoBytesToInt;

import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.GestureDescription.StrokeDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;


public class UtilsControl {

    public static final float setSigFigs = 1000.f;
    public static final int numBytesPerVal = 2;
    public static byte[] motionEventToBytes(MotionEvent motionEvent) {

        float normX1 = (motionEvent.getX()) / DISPLAY_SURFACE_WIDTH;
        float normY1 = (DISPLAY_SURFACE_HEIGHT - motionEvent.getY() ) / DISPLAY_SURFACE_HEIGHT;

        byte[] x1 = intToTwoBytes((int) (setSigFigs * normX1));
        byte[] y1 = intToTwoBytes((int) (setSigFigs * normY1));
        byte[] x2 = intToTwoBytes((int) (setSigFigs * normX1));
        byte[] y2 = intToTwoBytes((int) (setSigFigs * normY1));
        byte[] dur = intToTwoBytes(10);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(5 * numBytesPerVal);
        byteOutputStream.write(x1, 0, numBytesPerVal);
        byteOutputStream.write(y1, 0, numBytesPerVal);
        byteOutputStream.write(x2, 0, numBytesPerVal);
        byteOutputStream.write(y2, 0, numBytesPerVal);
        byteOutputStream.write(dur, 0, numBytesPerVal);

        return byteOutputStream.toByteArray();
    }

    public static StrokeDescription bytesToStrokeDescription(byte[] bytes) {

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
