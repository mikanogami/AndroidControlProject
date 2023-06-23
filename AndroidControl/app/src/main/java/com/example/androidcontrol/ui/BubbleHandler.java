package com.example.androidcontrol.ui;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.androidcontrol.service.FollowerService;

public class BubbleHandler implements View.OnClickListener {

    FollowerService service;

    public BubbleHandler(FollowerService service) { this.service = service; }
    public boolean onTouch(View view, MotionEvent motionEvent) {
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
        service.broadcastBubbleClicked();
    }

}
