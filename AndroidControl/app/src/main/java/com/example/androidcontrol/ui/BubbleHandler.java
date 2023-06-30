package com.example.androidcontrol.ui;

import static com.example.androidcontrol.utils.MyConstants.SCREEN_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.SCREEN_PIXELS_WIDTH;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Path;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Scroller;

import androidx.annotation.NonNull;

import com.example.androidcontrol.service.FollowerService;

public class BubbleHandler extends GestureDetector.SimpleOnGestureListener {
    private static final String TAG = "BubbleHandler";
    private FollowerService service;
    private float offsetX;
    private float offsetY;
    private Path scrollPath;
    private ObjectAnimator animator;
    public BubbleHandler(FollowerService followerService) {
        this.service = followerService;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(TAG,"onDown: " + event.toString());
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) service.serviceBubbleBinding.getRoot().getLayoutParams();
        offsetX = params.x - event.getRawX();
        offsetY = params.y - event.getRawY();
        //scrollPath = new Path();
        //scrollPath.moveTo(params.x, params.y);
        //animator = ObjectAnimator.ofFloat(service.serviceBubbleBinding.getRoot(), "x", "y", scrollPath);
        //animator.start();
        return true;
    }
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(TAG, "onSingleTapUp: " + event.toString());
        service.onBubbleClick();
        return true;
    }
    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        //Log.d(TAG, "onScroll: " + event1.toString() + event2.toString());

        service.mBubbleLayoutParams.x = (int) (offsetX + event2.getRawX());
        service.mBubbleLayoutParams.y = (int) (offsetY + event2.getRawY());
        service.getWindowManager().updateViewLayout(service.serviceBubbleBinding.getRoot(), service.mBubbleLayoutParams);

        Log.d(TAG, "onScroll: " + event2.getRawX() + " " + event2.getRawY());
        //scrollPath.lineTo(event2.getRawX(), event2.getRawY());
        //scrollPath.offset(distanceX, distanceY);

        //animator.setDuration(event2.getEventTime() - event1.getEventTime());




        return super.onScroll(event1, event2, distanceX, distanceY);
    }
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d(TAG, "onFling: " + event1.toString() + event2.toString());
        //mScroller.fling((int) event1.getRawX(),(int) event1.getRawY(), (int) velocityX,(int) velocityY, 0, 0, SCREEN_PIXELS_WIDTH, SCREEN_PIXELS_HEIGHT);
        return super.onFling(event1, event2, velocityX, velocityY);
    }


    /*
    public boolean onTouch(View view, MotionEvent motionEvent) {
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) view.getLayoutParams();

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("BubbleHandler", "onDown");
                break;
            case MotionEvent.ACTION_UP:
                Log.d("BubbleHandler", "onUp");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("BubbleHandler", "onMove");
                break;

        }

        return true;
    }

    @Override
    public void onClick(View view) {
        Log.d("BubbleHandler", "onClick");
        service.onBubbleClick();
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {
        Log.d("BubbleHandler", "onDrag");
        return false;
    }

     */
}