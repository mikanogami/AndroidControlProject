package com.example.androidcontrol;

import android.accessibilityservice.GestureDescription;
import android.graphics.PathMeasure;
import android.view.MotionEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.androidcontrol.utils.MyConstants;
import com.example.androidcontrol.utils.UtilsControl;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UtilsControlTests {

    public static MotionEvent test_ME_top_left;
    @Before
    public void setUp() {
        MyConstants.DISPLAY_SURFACE_WIDTH = 406;
        MyConstants.DISPLAY_SURFACE_HEIGHT = 764;

        MyConstants.VIDEO_PIXELS_WIDTH = 1080;
        MyConstants.VIDEO_PIXELS_HEIGHT = 2034;

        test_ME_top_left = MotionEvent.obtain(0, 10, MotionEvent.ACTION_DOWN, 0.01f, 0.01f, 0);
    }

    @Test
    public void test_click_top_left() {
        byte[] inputBytes = UtilsControl.motionEventToBytes(test_ME_top_left);
        GestureDescription.StrokeDescription clickStroke = UtilsControl.bytesToStrokeDescription(inputBytes);
        PathMeasure pm = new PathMeasure(clickStroke.getPath(), false);
        float[] pos = new float[2];
        pm.getPosTan(0, pos, null);
        System.out.println(pos[0] + " " + pos[1]);
    }
}