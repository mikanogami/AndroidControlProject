package com.example.androidcontrol.ui;

import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_HEIGHT;
import static com.example.androidcontrol.utils.MyConstants.VIDEO_PIXELS_WIDTH;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;

import com.example.androidcontrol.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        VIDEO_PIXELS_HEIGHT = displayMetrics.heightPixels;
        VIDEO_PIXELS_WIDTH = displayMetrics.widthPixels;

        binding.follower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), FollowerActivity.class);
                startActivity(intent);
            }
        });

        binding.expert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ExpertActivity.class);
                //Intent intent = new Intent(view.getContext(), ExpertActivityTest.class);
                startActivity(intent);
            }
        });
    }

}