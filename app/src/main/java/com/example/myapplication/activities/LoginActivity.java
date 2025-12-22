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
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.SessionManager;

import java.util.concurrent.Executor;

/**
 * Login activity for user authentication
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnBiometric;
    private TextView tvRegister, tvForgotPassword;
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
        setupClickListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnBiometric = findViewById(R.id.btn_biometric);
        tvRegister = findViewById(R.id.tv_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        // Check if biometric login is enabled
        if (sessionManager.isBiometricEnabled()) {
            btnBiometric.setVisibility(View.VISIBLE);
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }

    private void setupBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) 
                == BiometricManager.BIOMETRIC_SUCCESS) {
            
            Executor executor = ContextCompat.getMainExecutor(this);
            biometricPrompt = new BiometricPrompt(this, executor, 
                    new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(LoginActivity.this, 
                            "Xác thực thất bại: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    // Login with saved credentials
                    loginWithBiometric();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(LoginActivity.this, 
                            "Xác thực không thành công", Toast.LENGTH_SHORT).show();
                }
            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Đăng nhập bằng vân tay")
                    .setSubtitle("Sử dụng vân tay để đăng nhập vào TDTU Bank")
                    .setNegativeButtonText("Hủy")
                    .build();
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> validateAndLogin());

        btnBiometric.setOnClickListener(v -> {
            if (biometricPrompt != null && promptInfo != null) {
                biometricPrompt.authenticate(promptInfo);
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void validateAndLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Reset errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        // Perform login
        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        showLoading(true);

        firebaseHelper.signIn(email, password, task -> {
            if (task.isSuccessful()) {
                // Get user data from Firestore
                String userId = firebaseHelper.getCurrentUserId();
                loadUserData(userId);
            } else {
                showLoading(false);
                String errorMessage = "Đăng nhập thất bại";
                if (task.getException() != null) {
                    String message = task.getException().getMessage();
                    if (message != null && message.contains("password")) {
                        errorMessage = "Mật khẩu không đúng";
                    } else if (message != null && message.contains("no user")) {
                        errorMessage = "Tài khoản không tồn tại";
                    }
                }
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                        navigateToMain(user.getUserType());
                    }
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin người dùng", 
                            Toast.LENGTH_SHORT).show();
                }
            },
            e -> {
                showLoading(false);
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loginWithBiometric() {
        // Get saved user from session
        User savedUser = sessionManager.getUser();
        if (savedUser != null) {
            navigateToMain(savedUser.getUserType());
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập bằng email", Toast.LENGTH_SHORT).show();
        }
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

    private void showForgotPasswordDialog() {
        // Create a dialog to get email
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Quên mật khẩu");

        final EditText input = new EditText(this);
        input.setHint("Nhập email của bạn");
        builder.setView(input);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendPasswordResetEmail(email);
            } else {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        showLoading(true);
        firebaseHelper.sendPasswordResetEmail(email, task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Toast.makeText(this, 
                        "Đã gửi email đặt lại mật khẩu", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, 
                        "Không thể gửi email. Vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnBiometric.setEnabled(!show);
    }
}
