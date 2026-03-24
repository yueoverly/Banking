package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class BillPaymentActivity extends AppCompatActivity {
    
    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private Account userAccount;
    
    // Pending payment data
    private String pendingBillType;
    private String pendingBillCode;
    private double pendingAmount;

    // OTP Result Launcher
    private final ActivityResultLauncher<Intent> otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == OTPVerificationActivity.RESULT_OTP_VERIFIED) {
                    processBillPayment();
                } else {
                    Toast.makeText(this, "Giao dịch đã bị hủy", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_payment);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadUserAccount();

        findViewById(R.id.cardElectricity).setOnClickListener(v -> 
            showBillDialog("Điện", "EVN"));
        findViewById(R.id.cardWater).setOnClickListener(v -> 
            showBillDialog("Nước", "SAWACO"));
        findViewById(R.id.cardInternet).setOnClickListener(v -> 
            showBillDialog("Internet", "FPT/VNPT/Viettel"));
        findViewById(R.id.cardTV).setOnClickListener(v -> 
            showBillDialog("Truyền hình", "VTVcab/K+"));
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

    private void showBillDialog(String billType, String provider) {
        if (userAccount == null) {
            Toast.makeText(this, "Đang tải tài khoản...", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 0);

        TextView tvBalance = new TextView(this);
        tvBalance.setText("Số dư TK: " + FormatUtils.formatCurrency(userAccount.getBalance()));
        tvBalance.setTextColor(getResources().getColor(R.color.text_secondary, null));
        layout.addView(tvBalance);

        EditText etBillCode = new EditText(this);
        etBillCode.setHint("Mã khách hàng / Số hợp đồng");
        etBillCode.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etBillCode);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Số tiền thanh toán");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        new AlertDialog.Builder(this)
                .setTitle("Thanh toán " + billType)
                .setMessage("Nhà cung cấp: " + provider)
                .setView(layout)
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    String billCode = etBillCode.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();

                    if (billCode.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập mã khách hàng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (amount < 10000) {
                        Toast.makeText(this, "Số tiền tối thiểu 10,000 đ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (amount > userAccount.getBalance()) {
                        Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save pending data
                    pendingBillType = billType;
                    pendingBillCode = billCode;
                    pendingAmount = amount;

                    // Confirm and launch OTP
                    showConfirmDialog(billType, billCode, amount, provider);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showConfirmDialog(String billType, String billCode, double amount, String provider) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage("Loại: Hóa đơn " + billType +
                           "\nNhà cung cấp: " + provider +
                           "\nMã KH: " + billCode +
                           "\nSố tiền: " + FormatUtils.formatCurrency(amount))
                .setPositiveButton("Xác nhận", (d, w) -> launchOTPVerification())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void launchOTPVerification() {
        Intent intent = new Intent(this, OTPVerificationActivity.class);
        intent.putExtra(OTPVerificationActivity.EXTRA_TRANSACTION_TYPE, "BILL_PAYMENT");
        intent.putExtra(OTPVerificationActivity.EXTRA_AMOUNT, pendingAmount);
        intent.putExtra(OTPVerificationActivity.EXTRA_DESCRIPTION, "Hóa đơn " + pendingBillType);
        otpLauncher.launch(intent);
    }

    private void processBillPayment() {
        double newBalance = userAccount.getBalance() - pendingAmount;

        firebaseHelper.updateAccountBalance(userAccount.getId(), newBalance,
            aVoid -> {
                Transaction transaction = new Transaction();
                transaction.setFromAccountId(userAccount.getId());
                transaction.setAmount(pendingAmount);
                transaction.setType(Transaction.TransactionType.BILL_PAYMENT);
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setDescription("Thanh toán " + pendingBillType + " - " + pendingBillCode);

                firebaseHelper.createTransaction(transaction, task -> {
                    Toast.makeText(this, 
                        "✅ Thanh toán hóa đơn " + pendingBillType + " thành công!", 
                        Toast.LENGTH_LONG).show();
                    finish();
                });
            },
            e -> Toast.makeText(this, "Lỗi thanh toán", Toast.LENGTH_SHORT).show()
        );
    }
}
