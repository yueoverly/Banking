package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.Constants;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

/**
 * OTP Verification Activity for 2FA
 */
public class OTPVerificationActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvTransactionInfo, tvOtpSentTo, tvTimer;
    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private Button btnVerify, btnResend;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private Transaction transaction;
    private Account toAccount;
    private String generatedOtp;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        initViews();
        initFirebase();
        setupToolbar();
        getIntentData();
        setupOtpInputs();
        generateAndSendOtp();
        startTimer();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTransactionInfo = findViewById(R.id.tv_transaction_info);
        tvOtpSentTo = findViewById(R.id.tv_otp_sent_to);
        tvTimer = findViewById(R.id.tv_timer);
        etOtp1 = findViewById(R.id.et_otp_1);
        etOtp2 = findViewById(R.id.et_otp_2);
        etOtp3 = findViewById(R.id.et_otp_3);
        etOtp4 = findViewById(R.id.et_otp_4);
        etOtp5 = findViewById(R.id.et_otp_5);
        etOtp6 = findViewById(R.id.et_otp_6);
        btnVerify = findViewById(R.id.btn_verify);
        btnResend = findViewById(R.id.btn_resend);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Xác thực OTP");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void getIntentData() {
        transaction = (Transaction) getIntent().getSerializableExtra("transaction");
        toAccount = (Account) getIntent().getSerializableExtra("to_account");

        if (transaction != null) {
            String info = String.format("Chuyển %s đến tài khoản %s",
                    FormatUtils.formatCurrency(transaction.getAmount()),
                    transaction.getToAccountNumber());
            tvTransactionInfo.setText(info);
        }

        // Mask phone number
        User currentUser = sessionManager.getUser();
        if (currentUser != null && currentUser.getPhoneNumber() != null) {
            String phone = currentUser.getPhoneNumber();
            tvOtpSentTo.setText("Mã OTP đã được gửi đến số " + FormatUtils.maskPhoneNumber(phone));
        }
    }

    private void setupOtpInputs() {
        EditText[] otpFields = {etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6};

        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    checkOtpComplete();
                }
            });

            // Handle backspace
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && 
                    otpFields[index].getText().toString().isEmpty() && index > 0) {
                    otpFields[index - 1].requestFocus();
                    otpFields[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }

        btnVerify.setOnClickListener(v -> verifyOtp());
        btnResend.setOnClickListener(v -> {
            if (canResend) {
                generateAndSendOtp();
                startTimer();
            }
        });

        etOtp1.requestFocus();
    }

    private void checkOtpComplete() {
        String otp = getEnteredOtp();
        btnVerify.setEnabled(otp.length() == 6);
    }

    private String getEnteredOtp() {
        return etOtp1.getText().toString() +
               etOtp2.getText().toString() +
               etOtp3.getText().toString() +
               etOtp4.getText().toString() +
               etOtp5.getText().toString() +
               etOtp6.getText().toString();
    }

    private void generateAndSendOtp() {
        generatedOtp = firebaseHelper.generateOTP();
        
        // In a real app, send OTP via SMS
        // For demo, show OTP in toast
        Toast.makeText(this, "Mã OTP của bạn: " + generatedOtp, Toast.LENGTH_LONG).show();

        // Save OTP to Firebase
        firebaseHelper.saveOTP(transaction.getId(), generatedOtp, task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Lỗi gửi OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer() {
        canResend = false;
        btnResend.setEnabled(false);
        btnResend.setAlpha(0.5f);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(Constants.OTP_EXPIRY_MINUTES * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("Mã OTP hết hạn sau: %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Mã OTP đã hết hạn");
                canResend = true;
                btnResend.setEnabled(true);
                btnResend.setAlpha(1f);
            }
        }.start();
    }

    private void verifyOtp() {
        String enteredOtp = getEnteredOtp();
        
        if (enteredOtp.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Verify OTP
        if (enteredOtp.equals(generatedOtp)) {
            // OTP correct - process transaction
            transaction.setOtpVerified(true);
            
            if (transaction.requiresFaceVerification()) {
                // Need face verification
                Intent intent = new Intent(this, FaceVerificationActivity.class);
                intent.putExtra("transaction", transaction);
                intent.putExtra("to_account", toAccount);
                startActivity(intent);
                finish();
            } else {
                // Process transaction
                processTransaction();
            }
        } else {
            showLoading(false);
            Toast.makeText(this, "Mã OTP không đúng", Toast.LENGTH_SHORT).show();
            clearOtpFields();
        }
    }

    private void processTransaction() {
        if (transaction.getType() == Transaction.TransactionType.TRANSFER_INTERNAL && toAccount != null) {
            // Get from account
            firebaseHelper.getAccount(transaction.getFromAccountId(),
                doc -> {
                    Account fromAccount = doc.toObject(Account.class);
                    if (fromAccount != null) {
                        fromAccount.setId(doc.getId());
                        firebaseHelper.processTransfer(transaction, fromAccount, toAccount,
                            aVoid -> {
                                showLoading(false);
                                showSuccessAndFinish();
                            },
                            e -> {
                                showLoading(false);
                                Toast.makeText(this, "Giao dịch thất bại: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        );
                    }
                },
                e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            );
        } else {
            // External transfer
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            firebaseHelper.createTransaction(transaction, task -> {
                showLoading(false);
                if (task.isSuccessful()) {
                    showSuccessAndFinish();
                } else {
                    Toast.makeText(this, "Giao dịch thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showSuccessAndFinish() {
        Toast.makeText(this, "Giao dịch thành công!", Toast.LENGTH_SHORT).show();
        // Navigate to main
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void clearOtpFields() {
        etOtp1.setText("");
        etOtp2.setText("");
        etOtp3.setText("");
        etOtp4.setText("");
        etOtp5.setText("");
        etOtp6.setText("");
        etOtp1.requestFocus();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
