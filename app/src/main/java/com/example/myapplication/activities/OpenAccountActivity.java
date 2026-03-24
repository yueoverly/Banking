package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

import java.util.Date;

/**
 * OpenAccountActivity - Cho phép khách hàng mở tài khoản mới
 * - Tiết kiệm (Saving): Khách tự mở, tiền được trừ từ TK Thanh toán
 * - Vay thế chấp (Mortgage): Gửi yêu cầu, nhân viên duyệt
 */
public class OpenAccountActivity extends AppCompatActivity {

    private Button btnOpenSaving, btnRequestMortgage;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    
    private boolean hasSavingAccount = false;
    private boolean hasMortgageAccount = false;
    
    // Checking account info
    private Account checkingAccount = null;

    // Pending data for OTP
    private double pendingInitialDeposit;

    // OTP Result Launcher
    private final ActivityResultLauncher<Intent> otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == OTPVerificationActivity.RESULT_OTP_VERIFIED) {
                    createSavingAccountWithTransfer(pendingInitialDeposit);
                } else {
                    Toast.makeText(this, "Đã hủy mở tài khoản", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_account);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        setupToolbar();
        checkExistingAccounts();
    }

    private void initViews() {
        btnOpenSaving = findViewById(R.id.btnOpenSaving);
        btnRequestMortgage = findViewById(R.id.btnRequestMortgage);
        progressBar = findViewById(R.id.progressBar);

        btnOpenSaving.setOnClickListener(v -> showOpenSavingDialog());
        btnRequestMortgage.setOnClickListener(v -> showRequestMortgageDialog());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkExistingAccounts() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUserId();

        firebaseHelper.getUserAccounts(userId,
            querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                
                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null) {
                        account.setId(doc.getId());
                        
                        if (account.getAccountType() == Account.AccountType.CHECKING) {
                            checkingAccount = account;
                        } else if (account.getAccountType() == Account.AccountType.SAVING) {
                            hasSavingAccount = true;
                        } else if (account.getAccountType() == Account.AccountType.MORTGAGE) {
                            hasMortgageAccount = true;
                        }
                    }
                }

                updateUI();
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi kiểm tra tài khoản", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void updateUI() {
        if (hasSavingAccount) {
            btnOpenSaving.setText("✓ Đã có tài khoản Tiết kiệm");
            btnOpenSaving.setEnabled(false);
            btnOpenSaving.setAlpha(0.5f);
        }

        if (hasMortgageAccount) {
            btnRequestMortgage.setText("✓ Đã có tài khoản Vay");
            btnRequestMortgage.setEnabled(false);
            btnRequestMortgage.setAlpha(0.5f);
        }
    }

    private void showOpenSavingDialog() {
        if (hasSavingAccount) {
            Toast.makeText(this, "Bạn đã có tài khoản Tiết kiệm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user has checking account
        if (checkingAccount == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Không thể mở tài khoản")
                    .setMessage("Bạn cần có tài khoản Thanh toán để mở tài khoản Tiết kiệm.\n\nVui lòng liên hệ ngân hàng để được hỗ trợ.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Số tiền gửi ban đầu (tối thiểu 100,000đ)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        String message = "Lãi suất: 5.5%/năm\n\n" +
                "💳 Số dư TK Thanh toán: " + FormatUtils.formatCurrency(checkingAccount.getBalance()) + "\n\n" +
                "Nhập số tiền bạn muốn chuyển sang tài khoản tiết kiệm:";

        new AlertDialog.Builder(this)
                .setTitle("Mở tài khoản Tiết kiệm")
                .setMessage(message)
                .setView(input)
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    try {
                        double amount = Double.parseDouble(input.getText().toString());
                        if (amount < 100000) {
                            Toast.makeText(this, "Số tiền tối thiểu là 100,000đ", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (amount > checkingAccount.getBalance()) {
                            Toast.makeText(this, "Số dư TK Thanh toán không đủ!\nSố dư hiện tại: " + 
                                    FormatUtils.formatCurrency(checkingAccount.getBalance()), Toast.LENGTH_LONG).show();
                            return;
                        }
                        pendingInitialDeposit = amount;
                        launchOTPVerification(amount);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void launchOTPVerification(double amount) {
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra(OTPVerificationActivity.EXTRA_TRANSACTION_TYPE, "OPEN_SAVING");
        intent.putExtra(OTPVerificationActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(OTPVerificationActivity.EXTRA_DESCRIPTION, "Mở TK Tiết kiệm");
        otpLauncher.launch(intent);
    }

    /**
     * Tạo tài khoản tiết kiệm và chuyển tiền từ TK thanh toán
     */
    private void createSavingAccountWithTransfer(double initialDeposit) {
        progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUserId();

        // Step 1: Create saving account
        Account savingAccount = new Account(userId, Account.AccountType.SAVING);
        savingAccount.setBalance(initialDeposit);
        savingAccount.setInterestRate(5.5); // 5.5% per year
        savingAccount.setStatus(Account.AccountStatus.ACTIVE);
        savingAccount.setCreatedAt(new Date());
        savingAccount.setUpdatedAt(new Date());

        firebaseHelper.createAccount(savingAccount, createTask -> {
            if (createTask.isSuccessful()) {
                String savingAccountId = createTask.getResult().getId();
                
                // Step 2: Deduct from checking account
                double newCheckingBalance = checkingAccount.getBalance() - initialDeposit;
                firebaseHelper.updateAccountBalance(checkingAccount.getId(), newCheckingBalance,
                    unused -> {
                        // Step 3: Create transaction record
                        Transaction transaction = new Transaction();
                        transaction.setFromAccountId(checkingAccount.getId());
                        transaction.setToAccountId(savingAccountId);
                        transaction.setAmount(initialDeposit);
                        transaction.setType(Transaction.TransactionType.TRANSFER);
                        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                        transaction.setDescription("Mở tài khoản Tiết kiệm");
                        transaction.setRecipientName("TK Tiết kiệm");
                        transaction.setCreatedAt(new Date());

                        firebaseHelper.createTransaction(transaction, txTask -> {
                            progressBar.setVisibility(View.GONE);
                            
                            if (txTask.isSuccessful()) {
                                hasSavingAccount = true;
                                checkingAccount.setBalance(newCheckingBalance);
                                updateUI();

                                new AlertDialog.Builder(this)
                                        .setTitle("🎉 Thành công!")
                                        .setMessage("Tài khoản Tiết kiệm đã được mở!\n\n" +
                                                "💰 Số tiền gửi: " + FormatUtils.formatCurrency(initialDeposit) + "\n" +
                                                "📈 Lãi suất: 5.5%/năm\n" +
                                                "📊 Lợi nhuận dự kiến/tháng: " + FormatUtils.formatCurrency(initialDeposit * 0.055 / 12) + "\n\n" +
                                                "💳 Số dư TK Thanh toán còn lại: " + FormatUtils.formatCurrency(newCheckingBalance))
                                        .setPositiveButton("OK", (d, w) -> finish())
                                        .show();
                            } else {
                                showSuccessWithWarning(initialDeposit, newCheckingBalance);
                            }
                        });
                    },
                    e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Lỗi trừ tiền từ TK Thanh toán", Toast.LENGTH_SHORT).show();
                    }
                );
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tạo tài khoản Tiết kiệm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessWithWarning(double initialDeposit, double newCheckingBalance) {
        hasSavingAccount = true;
        checkingAccount.setBalance(newCheckingBalance);
        updateUI();

        new AlertDialog.Builder(this)
                .setTitle("🎉 Thành công!")
                .setMessage("Tài khoản Tiết kiệm đã được mở!\n\n" +
                        "💰 Số tiền gửi: " + FormatUtils.formatCurrency(initialDeposit) + "\n" +
                        "📈 Lãi suất: 5.5%/năm\n\n" +
                        "⚠️ Lưu ý: Giao dịch có thể chưa hiển thị trong lịch sử")
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    private void showRequestMortgageDialog() {
        if (hasMortgageAccount) {
            Toast.makeText(this, "Bạn đã có tài khoản Vay", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Gửi yêu cầu vay")
                .setMessage("Để mở tài khoản Vay thế chấp, bạn cần:\n\n" +
                           "1. Liên hệ chi nhánh TDT Bank gần nhất\n" +
                           "2. Chuẩn bị hồ sơ: CMND/CCCD, sổ hộ khẩu, giấy tờ tài sản thế chấp\n" +
                           "3. Nhân viên sẽ hỗ trợ tạo tài khoản vay cho bạn\n\n" +
                           "Bạn có muốn xem chi nhánh gần nhất?")
                .setPositiveButton("Xem bản đồ", (dialog, which) -> {
                    startActivity(new Intent(this, MapActivity.class));
                })
                .setNegativeButton("Để sau", null)
                .show();
    }
}
