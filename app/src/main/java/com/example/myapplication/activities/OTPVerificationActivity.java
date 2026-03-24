package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.myapplication.R;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.OTPManager;
import com.example.myapplication.utils.SessionManager;

public class OTPVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_TYPE = "transaction_type";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_DESCRIPTION = "description";
    public static final String EXTRA_EMAIL = "email"; // For registration
    public static final int RESULT_OTP_VERIFIED = 100;
    public static final int RESULT_OTP_CANCELLED = 101;

    private EditText etOTP;
    private TextView tvDescription, tvTimer, tvResend;
    private TextView tvTransactionType, tvTransactionAmount;
    private CardView cardTransactionInfo;
    private Button btnVerify, btnCancel;
    private ProgressBar progressBar;

    private OTPManager otpManager;
    private SessionManager sessionManager;
    private CountDownTimer countDownTimer;
    private String transactionType;
    private double amount;
    private String targetEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        initViews();
        otpManager = OTPManager.getInstance(this);
        sessionManager = SessionManager.getInstance(this);

        // Lấy thông tin từ Intent
        transactionType = getIntent().getStringExtra(EXTRA_TRANSACTION_TYPE);
        amount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        targetEmail = getIntent().getStringExtra(EXTRA_EMAIL);

        // Nếu không có email từ intent, lấy từ session
        if (targetEmail == null || targetEmail.isEmpty()) {
            targetEmail = sessionManager.getUserEmail();
        }

        setupTransactionInfo(description);
        setupListeners();
        sendOTP();
    }

    private void initViews() {
        etOTP = findViewById(R.id.etOTP);
        tvDescription = findViewById(R.id.tvDescription);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
        tvTransactionType = findViewById(R.id.tvTransactionType);
        tvTransactionAmount = findViewById(R.id.tvTransactionAmount);
        cardTransactionInfo = findViewById(R.id.cardTransactionInfo);
        btnVerify = findViewById(R.id.btnVerify);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupTransactionInfo(String description) {
        if (transactionType != null && amount > 0) {
            cardTransactionInfo.setVisibility(View.VISIBLE);
            
            String typeText;
            switch (transactionType) {
                case "TRANSFER":
                    typeText = "Chuyển tiền";
                    break;
                case "DEPOSIT":
                    typeText = "Nạp tiền";
                    break;
                case "WITHDRAW":
                    typeText = "Rút tiền";
                    break;
                case "BILL_PAYMENT":
                    typeText = "Thanh toán hóa đơn";
                    break;
                case "PHONE_TOPUP":
                    typeText = "Nạp điện thoại";
                    break;
                default:
                    typeText = "Giao dịch";
            }
            
            if (description != null && !description.isEmpty()) {
                typeText += " - " + description;
            }
            
            tvTransactionType.setText(typeText);
            tvTransactionAmount.setText(FormatUtils.formatCurrency(amount));
        }

        // Cập nhật mô tả với email
        if (targetEmail != null && !targetEmail.isEmpty()) {
            String maskedEmail = OTPManager.maskEmail(targetEmail);
            tvDescription.setText("Nhập mã OTP đã được gửi đến\nemail " + maskedEmail);
        } else {
            tvDescription.setText("Nhập mã OTP để xác minh giao dịch");
        }
    }

    private void setupListeners() {
        btnVerify.setOnClickListener(v -> verifyOTP());
        
        tvResend.setOnClickListener(v -> {
            if (tvResend.isEnabled()) {
                sendOTP();
            }
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_OTP_CANCELLED);
            finish();
        });
    }

    private void sendOTP() {
        // Disable resend button
        tvResend.setEnabled(false);
        tvResend.setTextColor(getResources().getColor(R.color.text_hint, null));
        
        progressBar.setVisibility(View.VISIBLE);

        // Gửi OTP qua email
        otpManager.sendOTPEmail(targetEmail, new OTPManager.OTPCallback() {
            @Override
            public void onOTPSent(String email) {
                progressBar.setVisibility(View.GONE);
                startCountdown();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                // Vẫn cho phép tiếp tục (demo mode đã hiển thị OTP qua Toast)
                startCountdown();
            }
        });
    }

    private void startCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(5 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("Mã có hiệu lực trong %d:%02d", minutes, seconds));
                tvTimer.setTextColor(getResources().getColor(R.color.text_secondary, null));
                
                // Enable resend sau 30 giây
                if (millisUntilFinished <= 4 * 60 * 1000 + 30 * 1000) {
                    tvResend.setEnabled(true);
                    tvResend.setTextColor(getResources().getColor(R.color.primary, null));
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Mã OTP đã hết hạn");
                tvTimer.setTextColor(getResources().getColor(R.color.error, null));
                tvResend.setEnabled(true);
                tvResend.setTextColor(getResources().getColor(R.color.primary, null));
            }
        }.start();
    }

    private void verifyOTP() {
        String inputOTP = etOTP.getText().toString().trim();

        if (inputOTP.length() != 6) {
            etOTP.setError("Vui lòng nhập đủ 6 số");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        // Simulate verification delay
        etOTP.postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnVerify.setEnabled(true);

            if (otpManager.verifyOTP(inputOTP)) {
                Toast.makeText(this, "✅ Xác minh thành công!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OTP_VERIFIED);
                finish();
            } else {
                etOTP.setError("Mã OTP không đúng hoặc đã hết hạn");
                Toast.makeText(this, "❌ Mã OTP không đúng", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OTP_CANCELLED);
        super.onBackPressed();
    }
}
