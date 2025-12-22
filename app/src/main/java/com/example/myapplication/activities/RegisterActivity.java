package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.Constants;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.SessionManager;

import java.util.Date;

/**
 * Registration activity for new users
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPhone, tilIdCard, tilPassword, tilConfirmPassword;
    private EditText etFullName, etEmail, etPhone, etIdCard, etPassword, etConfirmPassword;
    private RadioGroup rgUserType;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initFirebase();
        setupClickListeners();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilIdCard = findViewById(R.id.til_id_card);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etIdCard = findViewById(R.id.et_id_card);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        rgUserType = findViewById(R.id.rg_user_type);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> validateAndRegister());

        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void validateAndRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Reset errors
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilIdCard.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Validate full name
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            return;
        }

        // Validate phone
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            return;
        }
        if (!phone.matches(Constants.PHONE_REGEX)) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        // Validate ID card
        if (TextUtils.isEmpty(idCard)) {
            tilIdCard.setError("Vui lòng nhập số CMND/CCCD");
            return;
        }
        if (!idCard.matches(Constants.ID_CARD_REGEX)) {
            tilIdCard.setError("Số CMND/CCCD không hợp lệ");
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            tilPassword.setError("Mật khẩu phải có ít nhất " + Constants.MIN_PASSWORD_LENGTH + " ký tự");
            return;
        }

        // Validate confirm password
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu không khớp");
            return;
        }

        // Get user type
        User.UserType userType = User.UserType.CUSTOMER;
        int selectedId = rgUserType.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_officer) {
            userType = User.UserType.OFFICER;
        }

        // Create user
        performRegistration(fullName, email, phone, idCard, password, userType);
    }

    private void performRegistration(String fullName, String email, String phone, 
                                     String idCard, String password, User.UserType userType) {
        showLoading(true);

        firebaseHelper.signUp(email, password, task -> {
            if (task.isSuccessful()) {
                // Create user document in Firestore
                User user = new User(email, fullName, phone, userType);
                user.setId(firebaseHelper.getCurrentUserId());
                user.setIdCardNumber(idCard);
                user.setCreatedAt(new Date());
                user.setUpdatedAt(new Date());

                saveUserToFirestore(user);
            } else {
                showLoading(false);
                String errorMessage = "Đăng ký thất bại";
                if (task.getException() != null) {
                    String message = task.getException().getMessage();
                    if (message != null && message.contains("email")) {
                        errorMessage = "Email đã được sử dụng";
                    }
                }
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToFirestore(User user) {
        firebaseHelper.createUser(user, task -> {
            if (task.isSuccessful()) {
                // Create default checking account for customers
                if (user.getUserType() == User.UserType.CUSTOMER) {
                    createDefaultAccount(user);
                } else {
                    showLoading(false);
                    sessionManager.createLoginSession(user);
                    navigateToMain(user.getUserType());
                }
            } else {
                showLoading(false);
                Toast.makeText(RegisterActivity.this, 
                        "Lỗi lưu thông tin người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createDefaultAccount(User user) {
        Account checkingAccount = new Account(user.getId(), Account.AccountType.CHECKING);
        checkingAccount.setBalance(0);
        checkingAccount.setStatus(Account.AccountStatus.ACTIVE);

        firebaseHelper.createAccount(checkingAccount, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                sessionManager.createLoginSession(user);
                Toast.makeText(RegisterActivity.this, 
                        "Đăng ký thành công! Tài khoản của bạn: " + checkingAccount.getAccountNumber(), 
                        Toast.LENGTH_LONG).show();
                navigateToMain(user.getUserType());
            } else {
                sessionManager.createLoginSession(user);
                navigateToMain(user.getUserType());
            }
        });
    }

    private void navigateToMain(User.UserType userType) {
        Intent intent;
        if (userType == User.UserType.OFFICER) {
            intent = new Intent(this, OfficerMainActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}
