package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.OTPManager;
import com.example.myapplication.utils.SessionManager;

import java.util.Date;

/**
 * RegisterActivity - Màn hình đăng ký cho KHÁCH HÀNG
 * 
 * Lưu ý:
 * - Chỉ khách hàng mới có thể tự đăng ký
 * - Nhân viên ngân hàng được admin tạo sẵn trong Firebase
 * - Hệ thống tự động set userType = CUSTOMER
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etIdCard, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private OTPManager otpManager;

    // Pending registration data
    private String pendingFullName, pendingEmail, pendingPhone, pendingIdCard, pendingPassword;

    // OTP Result Launcher
    private final ActivityResultLauncher<Intent> otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == OTPVerificationActivity.RESULT_OTP_VERIFIED) {
                    performRegistration();
                } else {
                    Toast.makeText(this, "Đăng ký đã bị hủy", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        otpManager = OTPManager.getInstance(this);

        btnRegister.setOnClickListener(v -> validateAndRequestOTP());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etIdCard = findViewById(R.id.etIdCard);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void validateAndRequestOTP() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String idCard = etIdCard.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ tên");
            etFullName.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            etPhone.setError("Số điện thoại không hợp lệ");
            etPhone.requestFocus();
            return;
        }

        if (idCard.length() < 9) {
            etIdCard.setError("Số CMND/CCCD không hợp lệ");
            etIdCard.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Save pending data
        pendingFullName = fullName;
        pendingEmail = email;
        pendingPhone = phone;
        pendingIdCard = idCard;
        pendingPassword = password;

        // Launch OTP verification with email
        launchOTPVerification(email);
    }

    private void launchOTPVerification(String email) {
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra(OTPVerificationActivity.EXTRA_TRANSACTION_TYPE, "REGISTER");
        intent.putExtra(OTPVerificationActivity.EXTRA_DESCRIPTION, "Xác minh email");
        intent.putExtra(OTPVerificationActivity.EXTRA_EMAIL, email);
        otpLauncher.launch(intent);
    }

    private void performRegistration() {
        showLoading(true);

        firebaseHelper.signUp(pendingEmail, pendingPassword, task -> {
            if (task.isSuccessful()) {
                // Tạo user với userType = CUSTOMER (mặc định)
                User user = new User(pendingEmail, pendingFullName, pendingPhone, User.UserType.CUSTOMER);
                user.setId(firebaseHelper.getCurrentUserId());
                user.setIdCardNumber(pendingIdCard);
                user.setCreatedAt(new Date());
                user.setUpdatedAt(new Date());
                user.setVerified(true);
                saveUser(user);
            } else {
                showLoading(false);
                String error = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUser(User user) {
        firebaseHelper.createUser(user, task -> {
            if (task.isSuccessful()) {
                // Tạo tài khoản thanh toán mặc định cho khách hàng
                createDefaultAccount(user);
            } else {
                showLoading(false);
                Toast.makeText(this, "Lỗi lưu thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createDefaultAccount(User user) {
        Account account = new Account(user.getId(), Account.AccountType.CHECKING);
        account.setBalance(1000000); // 1 triệu VND khởi điểm
        account.setStatus(Account.AccountStatus.ACTIVE);

        firebaseHelper.createAccount(account, task -> {
            showLoading(false);
            sessionManager.createLoginSession(user);
            Toast.makeText(this, "✅ Đăng ký thành công!", Toast.LENGTH_SHORT).show();
            
            // Luôn chuyển đến MainActivity (khách hàng)
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}
