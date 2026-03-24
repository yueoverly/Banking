package com.example.myapplication.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvName = findViewById(R.id.tvName);
        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvIdCard = findViewById(R.id.tvIdCard);

        User user = SessionManager.getInstance(this).getUser();
        if (user != null) {
            tvName.setText(user.getFullName());
            tvEmail.setText(user.getEmail());
            tvPhone.setText(user.getPhone());
        }

        FirebaseHelper.getInstance().getUser(SessionManager.getInstance(this).getUserId(),
            doc -> {
                if (doc.exists()) {
                    tvIdCard.setText(doc.getString("idCardNumber"));
                }
            },
            e -> {}
        );
    }
}
