package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.myapplication.R;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.SessionManager;

public class OfficerMainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvOfficerName, tvCustomerCount, tvAccountCount;
    private CardView cardCreateCustomer, cardManageCustomers, cardManageRates;
    private Button btnLogout;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_main);

        initViews();
        initFirebase();
        setupListeners();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvOfficerName = findViewById(R.id.tvOfficerName);
        tvCustomerCount = findViewById(R.id.tvCustomerCount);
        tvAccountCount = findViewById(R.id.tvAccountCount);
        cardCreateCustomer = findViewById(R.id.cardCreateCustomer);
        cardManageCustomers = findViewById(R.id.cardManageCustomers);
        cardManageRates = findViewById(R.id.cardManageRates);
        btnLogout = findViewById(R.id.btnLogout);
        progressBar = findViewById(R.id.progressBar);

        setSupportActionBar(toolbar);
    }

    private void initFirebase() {
        sessionManager = SessionManager.getInstance(this);
        firebaseHelper = FirebaseHelper.getInstance();

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupListeners() {
        cardCreateCustomer.setOnClickListener(v -> 
            startActivity(new Intent(this, CreateCustomerActivity.class)));

        cardManageCustomers.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageCustomersActivity.class)));

        cardManageRates.setOnClickListener(v -> 
            startActivity(new Intent(this, ManageInterestRatesActivity.class)));

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadData() {
        tvOfficerName.setText(sessionManager.getUserName());
        loadStats();
    }

    private void loadStats() {
        firebaseHelper.getAllCustomers(
            querySnapshot -> {
                int count = querySnapshot.size();
                tvCustomerCount.setText(String.valueOf(count));
                
                // Load account count
                final int[] accountCount = {0};
                if (count == 0) {
                    tvAccountCount.setText("0");
                } else {
                    for (var doc : querySnapshot.getDocuments()) {
                        String customerId = doc.getId();
                        firebaseHelper.getUserAccounts(customerId,
                            accounts -> {
                                accountCount[0] += accounts.size();
                                tvAccountCount.setText(String.valueOf(accountCount[0]));
                            },
                            e -> {}
                        );
                    }
                }
            },
            e -> Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
        );
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    sessionManager.logout();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
