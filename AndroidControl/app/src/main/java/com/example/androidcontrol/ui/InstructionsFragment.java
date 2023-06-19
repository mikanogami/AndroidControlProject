package com.example.androidcontrol.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidcontrol.R;
import com.example.androidcontrol.databinding.FragmentInstructionsBinding;
import com.example.androidcontrol.databinding.FragmentMainBinding;

public class InstructionsFragment extends Fragment {

    FragmentInstructionsBinding binding;
    ActionBar actionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentInstructionsBinding.inflate(inflater, container, false);
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.instructions_page_title);
        actionBar.setDisplayHomeAsUpEnabled(true);


        return binding.getRoot();
    }



}
