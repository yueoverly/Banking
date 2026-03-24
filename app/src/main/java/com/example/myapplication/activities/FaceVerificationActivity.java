package com.example.myapplication.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.myapplication.R;

public class FaceVerificationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_verification);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.btnCapture).setOnClickListener(v -> {
            Toast.makeText(this, "Xác minh khuôn mặt thành công", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }
}
