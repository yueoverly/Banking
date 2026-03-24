package com.example.myapplication.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

public class PhoneTopUpActivity extends AppCompatActivity {
    private EditText etPhone;
    private Button btnTopUp;
    private GridLayout gridAmounts;
    
    private double selectedAmount = 0;
    private Button selectedButton = null;
    private Account userAccount;
    
    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;

    // OTP Result Launcher
    private final ActivityResultLauncher<Intent> otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == OTPVerificationActivity.RESULT_OTP_VERIFIED) {
                    processTopUp();
                } else {
                    Toast.makeText(this, "Giao dịch đã bị hủy", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_topup);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etPhone = findViewById(R.id.etPhone);
        btnTopUp = findViewById(R.id.btnTopUp);
        gridAmounts = findViewById(R.id.gridAmounts);

        // Pre-fill user's phone
        String userPhone = sessionManager.getUserPhone();
        if (userPhone != null && !userPhone.isEmpty()) {
            etPhone.setText(userPhone);
        }

        setupAmountButtons();
        loadUserAccount();

        btnTopUp.setOnClickListener(v -> validateAndTopUp());
    }

    private void setupAmountButtons() {
        double[] amounts = {10000, 20000, 50000, 100000, 200000, 500000};
        
        for (double amount : amounts) {
            Button btn = new Button(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 140;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);
            btn.setText(FormatUtils.formatCurrencyNoSymbol(amount));
            btn.setTextSize(14);
            btn.setBackgroundResource(R.drawable.bg_button_outline);
            btn.setTextColor(getResources().getColor(R.color.primary, null));

            btn.setOnClickListener(v -> {
                // Deselect previous
                if (selectedButton != null) {
                    selectedButton.setBackgroundResource(R.drawable.bg_button_outline);
                    selectedButton.setTextColor(getResources().getColor(R.color.primary, null));
                }
                
                // Select current
                selectedAmount = amount;
                selectedButton = btn;
                btn.setBackgroundResource(R.drawable.bg_button_primary);
                btn.setTextColor(Color.WHITE);
            });

            gridAmounts.addView(btn);
        }
    }

    private void loadUserAccount() {
        String userId = sessionManager.getUserId();
        firebaseHelper.getUserAccounts(userId,
            querySnapshot -> {
                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null && account.getAccountType() == Account.AccountType.CHECKING) {
                        account.setId(doc.getId());
                        userAccount = account;
                        break;
                    }
                }
            },
            e -> Toast.makeText(this, "Lỗi tải tài khoản", Toast.LENGTH_SHORT).show()
        );
    }

    private void validateAndTopUp() {
        String phone = etPhone.getText().toString().trim();

        if (phone.length() < 10) {
            etPhone.setError("Số điện thoại không hợp lệ");
            return;
        }

        if (selectedAmount == 0) {
            Toast.makeText(this, "Vui lòng chọn mệnh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userAccount == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAmount > userAccount.getBalance()) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Confirm dialog
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận nạp điện thoại")
                .setMessage("Nạp " + FormatUtils.formatCurrency(selectedAmount) + 
                           "\ncho số: " + phone +
                           "\n\nSố dư TK: " + FormatUtils.formatCurrency(userAccount.getBalance()))
                .setPositiveButton("Xác nhận", (d, w) -> launchOTPVerification())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void launchOTPVerification() {
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra(OTPVerificationActivity.EXTRA_TRANSACTION_TYPE, "PHONE_TOPUP");
        intent.putExtra(OTPVerificationActivity.EXTRA_AMOUNT, selectedAmount);
        intent.putExtra(OTPVerificationActivity.EXTRA_DESCRIPTION, etPhone.getText().toString());
        otpLauncher.launch(intent);
    }

    private void processTopUp() {
        String phone = etPhone.getText().toString().trim();
        
        btnTopUp.setEnabled(false);
        double newBalance = userAccount.getBalance() - selectedAmount;

        firebaseHelper.updateAccountBalance(userAccount.getId(), newBalance,
            aVoid -> {
                Transaction transaction = new Transaction();
                transaction.setFromAccountId(userAccount.getId());
                transaction.setAmount(selectedAmount);
                transaction.setType(Transaction.TransactionType.PHONE_TOPUP);
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setDescription("Nạp điện thoại: " + phone);

                firebaseHelper.createTransaction(transaction, task -> {
                    btnTopUp.setEnabled(true);
                    Toast.makeText(this, 
                        "✅ Nạp " + FormatUtils.formatCurrency(selectedAmount) + " cho " + phone + " thành công!", 
                        Toast.LENGTH_LONG).show();
                    finish();
                });
            },
            e -> {
                btnTopUp.setEnabled(true);
                Toast.makeText(this, "Lỗi nạp tiền", Toast.LENGTH_SHORT).show();
            }
        );
    }
}
