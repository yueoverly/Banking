package com.example.myapplication;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class BankingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
