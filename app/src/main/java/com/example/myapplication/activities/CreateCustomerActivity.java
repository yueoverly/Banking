package com.example.myapplication.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import java.util.Date;

public class CreateCustomerActivity extends AppCompatActivity {
    private EditText etFullName, etEmail, etPhone, etIdCard, etPassword;
    private Button btnCreate;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_customer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etIdCard = findViewById(R.id.etIdCard);
        etPassword = findViewById(R.id.etPassword);
        btnCreate = findViewById(R.id.btnCreate);
        progressBar = findViewById(R.id.progressBar);

        firebaseHelper = FirebaseHelper.getInstance();
        btnCreate.setOnClickListener(v -> createCustomer());
    }

    private void createCustomer() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || 
            TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);

        firebaseHelper.signUp(email, password, task -> {
            if (task.isSuccessful()) {
                User user = new User(email, fullName, phone, User.UserType.CUSTOMER);
                user.setId(firebaseHelper.getCurrentUserId());
                user.setIdCardNumber(idCard);
                user.setCreatedAt(new Date());

                firebaseHelper.createUser(user, userTask -> {
                    Account account = new Account(user.getId(), Account.AccountType.CHECKING);
                    account.setBalance(0);

                    firebaseHelper.createAccount(account, accTask -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Tạo khách hàng thành công", Toast.LENGTH_SHORT).show();
                        firebaseHelper.signOut();
                        finish();
                    });
                });
            } else {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);
                Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
