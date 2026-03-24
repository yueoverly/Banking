package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class TransferActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvFromAccount, tvFromBalance, tvRecipientName;
    private EditText etToAccount, etAmount, etDescription;
    private Button btnTransfer;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private Account fromAccount;
    private Account toAccount;
    
    // Pending transaction data
    private double pendingAmount;
    private String pendingDescription;

    // OTP Result Launcher
    private final ActivityResultLauncher<Intent> otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == OTPVerificationActivity.RESULT_OTP_VERIFIED) {
                    // OTP verified - proceed with transfer
                    performTransfer(pendingAmount, pendingDescription);
                } else {
                    Toast.makeText(this, "Giao dịch đã bị hủy", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        setupToolbar();
        setupListeners();
        loadFromAccount();
        
        // Handle QR data from QRScannerActivity
        handleQRData();
    }

    private void handleQRData() {
        Intent intent = getIntent();
        String recipientAccountNumber = intent.getStringExtra("recipient_account_number");
        String recipientName = intent.getStringExtra("recipient_name");
        String recipientAccountId = intent.getStringExtra("recipient_account_id");

        if (recipientAccountNumber != null && !recipientAccountNumber.isEmpty()) {
            etToAccount.setText(recipientAccountNumber);
            
            if (recipientName != null && !recipientName.isEmpty()) {
                tvRecipientName.setText("👤 " + recipientName);
                tvRecipientName.setVisibility(View.VISIBLE);
            }
            
            // If we have accountId, load the account directly
            if (recipientAccountId != null && !recipientAccountId.isEmpty()) {
                firebaseHelper.getAccount(recipientAccountId,
                    doc -> {
                        toAccount = doc.toObject(Account.class);
                        if (toAccount != null) {
                            toAccount.setId(doc.getId());
                        }
                    },
                    e -> {
                        // Fallback to search by number
                        searchRecipient(recipientAccountNumber);
                    }
                );
            } else {
                searchRecipient(recipientAccountNumber);
            }
            
            // Focus on amount field
            etAmount.requestFocus();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvFromAccount = findViewById(R.id.tvFromAccount);
        tvFromBalance = findViewById(R.id.tvFromBalance);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        etToAccount = findViewById(R.id.etToAccount);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        btnTransfer = findViewById(R.id.btnTransfer);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        etToAccount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 16) {
                    searchRecipient(s.toString());
                } else {
                    tvRecipientName.setVisibility(View.GONE);
                    toAccount = null;
                }
            }
        });

        btnTransfer.setOnClickListener(v -> validateAndTransfer());
    }

    private void loadFromAccount() {
        String userId = sessionManager.getUserId();
        firebaseHelper.getUserAccounts(userId,
            querySnapshot -> {
                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null && account.getAccountType() == Account.AccountType.CHECKING) {
                        account.setId(doc.getId());
                        fromAccount = account;
                        tvFromAccount.setText("TK: " + account.getFormattedAccountNumber());
                        tvFromBalance.setText("Số dư: " + FormatUtils.formatCurrency(account.getBalance()));
                        break;
                    }
                }
            },
            e -> Toast.makeText(this, "Lỗi tải tài khoản", Toast.LENGTH_SHORT).show()
        );
    }

    private void searchRecipient(String accountNumber) {
        firebaseHelper.searchAccountByNumber(accountNumber,
            querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    var doc = querySnapshot.getDocuments().get(0);
                    toAccount = doc.toObject(Account.class);
                    if (toAccount != null) {
                        toAccount.setId(doc.getId());
                        firebaseHelper.getUser(toAccount.getUserId(),
                            userDoc -> {
                                if (userDoc.exists()) {
                                    String name = userDoc.getString("fullName");
                                    tvRecipientName.setText("👤 Người nhận: " + name);
                                    tvRecipientName.setVisibility(View.VISIBLE);
                                }
                            },
                            e -> {}
                        );
                    }
                }
            },
            e -> {}
        );
    }

    private void validateAndTransfer() {
        if (fromAccount == null) {
            Toast.makeText(this, "Không có tài khoản nguồn", Toast.LENGTH_SHORT).show();
            return;
        }

        String toAccountNumber = etToAccount.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(toAccountNumber) || toAccountNumber.length() != 16) {
            etToAccount.setError("Số tài khoản phải có 16 số");
            return;
        }

        if (toAccount == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản người nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (toAccount.getId().equals(fromAccount.getId())) {
            Toast.makeText(this, "Không thể chuyển tiền cho chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 10000) {
            etAmount.setError("Số tiền tối thiểu 10,000 đ");
            return;
        }

        if (amount > fromAccount.getBalance()) {
            etAmount.setError("Số dư không đủ");
            return;
        }

        // Confirm and request OTP
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận chuyển tiền")
                .setMessage("Chuyển " + FormatUtils.formatCurrency(amount) + 
                           "\nđến: " + tvRecipientName.getText().toString().replace("👤 ", "") +
                           "\nSố TK: " + toAccountNumber)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    // Save pending data and launch OTP
                    pendingAmount = amount;
                    pendingDescription = description.isEmpty() ? "Chuyển tiền" : description;
                    launchOTPVerification(amount, pendingDescription);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void launchOTPVerification(double amount, String description) {
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra(OTPVerificationActivity.EXTRA_TRANSACTION_TYPE, "TRANSFER");
        intent.putExtra(OTPVerificationActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(OTPVerificationActivity.EXTRA_DESCRIPTION, description);
        otpLauncher.launch(intent);
    }

    private void performTransfer(double amount, String description) {
        showLoading(true);

        double newFromBalance = fromAccount.getBalance() - amount;
        firebaseHelper.updateAccountBalance(fromAccount.getId(), newFromBalance,
            aVoid -> {
                double newToBalance = toAccount.getBalance() + amount;
                firebaseHelper.updateAccountBalance(toAccount.getId(), newToBalance,
                    aVoid2 -> {
                        Transaction transaction = new Transaction();
                        transaction.setFromAccountId(fromAccount.getId());
                        transaction.setToAccountId(toAccount.getId());
                        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
                        transaction.setToAccountNumber(toAccount.getAccountNumber());
                        transaction.setAmount(amount);
                        transaction.setType(Transaction.TransactionType.TRANSFER);
                        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                        transaction.setDescription(description);

                        firebaseHelper.createTransaction(transaction, task -> {
                            showLoading(false);
                            Toast.makeText(this, "✅ Chuyển tiền thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    },
                    e -> {
                        showLoading(false);
                        Toast.makeText(this, "Lỗi chuyển tiền", Toast.LENGTH_SHORT).show();
                    }
                );
            },
            e -> {
                showLoading(false);
                Toast.makeText(this, "Lỗi chuyển tiền", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnTransfer.setEnabled(!show);
    }
}
