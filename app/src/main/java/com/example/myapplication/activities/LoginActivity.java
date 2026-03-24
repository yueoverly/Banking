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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.SessionManager;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnBiometric;
    private TextView tvForgotPassword, tvRegister;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initFirebase();
        setupBiometric();
        setupListeners();

        if (sessionManager.isLoggedIn()) {
            navigateToMain();
        }
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometric = findViewById(R.id.btnBiometric);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
    }

    private void setupBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
                == BiometricManager.BIOMETRIC_SUCCESS) {
            
            Executor executor = ContextCompat.getMainExecutor(this);
            biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    if (sessionManager.getUser() != null) {
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Vui lòng đăng nhập bằng email trước", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    Toast.makeText(LoginActivity.this, errString, Toast.LENGTH_SHORT).show();
                }
            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Đăng nhập TDT Bank")
                    .setSubtitle("Sử dụng vân tay để đăng nhập")
                    .setNegativeButtonText("Hủy")
                    .build();
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> validateAndLogin());
        
        btnBiometric.setOnClickListener(v -> {
            if (biometricPrompt != null && promptInfo != null) {
                biometricPrompt.authenticate(promptInfo);
            }
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void validateAndLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        showLoading(true);
        
        firebaseHelper.signIn(email, password, task -> {
            if (task.isSuccessful()) {
                loadUserData(firebaseHelper.getCurrentUserId());
            } else {
                showLoading(false);
                String error = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData(String userId) {
        firebaseHelper.getUser(userId,
            documentSnapshot -> {
                showLoading(false);
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(documentSnapshot.getId());
                        sessionManager.createLoginSession(user);
                        navigateToMain();
                    }
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            },
            e -> {
                showLoading(false);
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void navigateToMain() {
        User user = sessionManager.getUser();
        Intent intent;
        if (user != null && user.getUserType() == User.UserType.OFFICER) {
            intent = new Intent(this, OfficerMainActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập email của bạn");
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Quên mật khẩu")
                .setView(input)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        firebaseHelper.sendPasswordResetEmail(email, task -> {
                            String msg = task.isSuccessful() ? "Đã gửi email đặt lại mật khẩu" : "Không thể gửi email";
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnBiometric.setEnabled(!show);
    }
}
