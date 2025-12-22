package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.SessionManager;

/**
 * Splash screen activity shown at app launch
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ImageView logoImage = findViewById(R.id.img_logo);
        TextView appName = findViewById(R.id.tv_app_name);
        TextView tagline = findViewById(R.id.tv_tagline);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);

        // Start animations
        logoImage.startAnimation(fadeIn);
        appName.startAnimation(fadeIn);
        tagline.startAnimation(fadeIn);

        // Navigate to appropriate screen after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, SPLASH_DURATION);
    }

    private void navigateToNextScreen() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            // User is logged in, check user type
            User.UserType userType = sessionManager.getUserType();
            if (userType == User.UserType.OFFICER) {
                intent = new Intent(this, OfficerMainActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            // User is not logged in
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
