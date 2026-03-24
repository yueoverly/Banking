package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, SPLASH_DELAY);
    }

    private void checkLoginStatus() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            User user = sessionManager.getUser();
            if (user != null && user.getUserType() == User.UserType.OFFICER) {
                intent = new Intent(this, OfficerMainActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
