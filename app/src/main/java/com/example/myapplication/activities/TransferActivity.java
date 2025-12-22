package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.Constants;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for money transfer functionality
 */
public class TransferActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerFromAccount;
    private RadioGroup rgTransferType;
    private Spinner spinnerBank;
    private TextInputLayout tilAccountNumber, tilAmount, tilDescription;
    private EditText etAccountNumber, etAmount, etDescription;
    private TextView tvRecipientName, tvAvailableBalance, tvFee, tvTotalAmount;
    private Button btnTransfer;
    private ProgressBar progressBar;
    private ImageButton btnScanQR;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private User currentUser;
    private List<Account> userAccounts;
    private Account selectedFromAccount;
    private Account recipientAccount;
    private double transferFee = 0;
    private boolean isInternalTransfer = true;

    private static final int REQUEST_QR_SCAN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        initViews();
        initFirebase();
        setupToolbar();
        setupListeners();
        loadUserAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerFromAccount = findViewById(R.id.spinner_from_account);
        rgTransferType = findViewById(R.id.rg_transfer_type);
        spinnerBank = findViewById(R.id.spinner_bank);
        tilAccountNumber = findViewById(R.id.til_account_number);
        tilAmount = findViewById(R.id.til_amount);
        tilDescription = findViewById(R.id.til_description);
        etAccountNumber = findViewById(R.id.et_account_number);
        etAmount = findViewById(R.id.et_amount);
        etDescription = findViewById(R.id.et_description);
        tvRecipientName = findViewById(R.id.tv_recipient_name);
        tvAvailableBalance = findViewById(R.id.tv_available_balance);
        tvFee = findViewById(R.id.tv_fee);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        btnTransfer = findViewById(R.id.btn_transfer);
        progressBar = findViewById(R.id.progress_bar);
        btnScanQR = findViewById(R.id.btn_scan_qr);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        currentUser = sessionManager.getUser();
        userAccounts = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chuyển tiền");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        // Transfer type selection
        rgTransferType.setOnCheckedChangeListener((group, checkedId) -> {
            isInternalTransfer = checkedId == R.id.rb_internal;
            spinnerBank.setVisibility(isInternalTransfer ? View.GONE : View.VISIBLE);
            updateFee();
        });

        // From account selection
        spinnerFromAccount.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position < userAccounts.size()) {
                    selectedFromAccount = userAccounts.get(position);
                    tvAvailableBalance.setText("Số dư khả dụng: " + 
                            FormatUtils.formatCurrency(selectedFromAccount.getBalance()));
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Account number input - search for recipient
        etAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 10 && isInternalTransfer) {
                    searchRecipient(s.toString());
                } else {
                    tvRecipientName.setVisibility(View.GONE);
                }
            }
        });

        // Amount input - update total
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateTotalAmount();
            }
        });

        // QR scan button
        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScannerActivity.class);
            startActivityForResult(intent, REQUEST_QR_SCAN);
        });

        // Transfer button
        btnTransfer.setOnClickListener(v -> validateAndTransfer());

        // Setup bank spinner
        String[] banks = {"Vietcombank", "BIDV", "Techcombank", "VPBank", "MBBank", "ACB", "Sacombank"};
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, banks);
        bankAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBank.setAdapter(bankAdapter);
        spinnerBank.setVisibility(View.GONE);
    }

    private void loadUserAccounts() {
        if (currentUser == null) return;

        showLoading(true);
        firebaseHelper.getUserAccounts(currentUser.getId(),
            querySnapshot -> {
                showLoading(false);
                userAccounts.clear();
                List<String> accountNames = new ArrayList<>();

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null && account.getAccountType() == Account.AccountType.CHECKING) {
                        account.setId(doc.getId());
                        userAccounts.add(account);
                        accountNames.add(account.getAccountNumber() + " - " + 
                                FormatUtils.formatCurrency(account.getBalance()));
                    }
                }

                if (userAccounts.isEmpty()) {
                    Toast.makeText(this, "Bạn chưa có tài khoản thanh toán", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, accountNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerFromAccount.setAdapter(adapter);

                selectedFromAccount = userAccounts.get(0);
                tvAvailableBalance.setText("Số dư khả dụng: " + 
                        FormatUtils.formatCurrency(selectedFromAccount.getBalance()));
            },
            e -> {
                showLoading(false);
                Toast.makeText(this, "Lỗi tải danh sách tài khoản", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void searchRecipient(String accountNumber) {
        firebaseHelper.getAccountByNumber(accountNumber,
            querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    recipientAccount = doc.toObject(Account.class);
                    if (recipientAccount != null) {
                        recipientAccount.setId(doc.getId());
                        // Get recipient user name
                        firebaseHelper.getUser(recipientAccount.getUserId(),
                            userDoc -> {
                                if (userDoc.exists()) {
                                    User recipientUser = userDoc.toObject(User.class);
                                    if (recipientUser != null) {
                                        tvRecipientName.setText("Người nhận: " + recipientUser.getFullName());
                                        tvRecipientName.setVisibility(View.VISIBLE);
                                    }
                                }
                            },
                            e -> {}
                        );
                    }
                } else {
                    recipientAccount = null;
                    tvRecipientName.setVisibility(View.GONE);
                }
            },
            e -> {
                recipientAccount = null;
                tvRecipientName.setVisibility(View.GONE);
            }
        );
    }

    private void updateFee() {
        if (isInternalTransfer) {
            transferFee = 0;
        } else {
            transferFee = 11000; // External transfer fee
        }
        tvFee.setText("Phí chuyển khoản: " + FormatUtils.formatCurrency(transferFee));
        updateTotalAmount();
    }

    private void updateTotalAmount() {
        String amountStr = etAmount.getText().toString().trim();
        double amount = 0;
        if (!TextUtils.isEmpty(amountStr)) {
            try {
                amount = Double.parseDouble(amountStr.replaceAll("[^\\d]", ""));
            } catch (NumberFormatException e) {
                amount = 0;
            }
        }
        double total = amount + transferFee;
        tvTotalAmount.setText("Tổng tiền: " + FormatUtils.formatCurrency(total));
    }

    private void validateAndTransfer() {
        String accountNumber = etAccountNumber.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Reset errors
        tilAccountNumber.setError(null);
        tilAmount.setError(null);

        // Validate
        if (TextUtils.isEmpty(accountNumber)) {
            tilAccountNumber.setError("Vui lòng nhập số tài khoản người nhận");
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            tilAmount.setError("Vui lòng nhập số tiền");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException e) {
            tilAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 10000) {
            tilAmount.setError("Số tiền tối thiểu là 10,000 VND");
            return;
        }

        if (amount > Constants.TRANSACTION_LIMIT_SINGLE) {
            tilAmount.setError("Số tiền vượt quá hạn mức");
            return;
        }

        double totalAmount = amount + transferFee;
        if (totalAmount > selectedFromAccount.getBalance()) {
            tilAmount.setError("Số dư không đủ");
            return;
        }

        if (isInternalTransfer && recipientAccount == null) {
            tilAccountNumber.setError("Không tìm thấy tài khoản người nhận");
            return;
        }

        if (isInternalTransfer && recipientAccount.getId().equals(selectedFromAccount.getId())) {
            tilAccountNumber.setError("Không thể chuyển cho chính mình");
            return;
        }

        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(selectedFromAccount.getId());
        transaction.setFromAccountNumber(selectedFromAccount.getAccountNumber());
        transaction.setToAccountNumber(accountNumber);
        transaction.setAmount(amount);
        transaction.setFee(transferFee);
        transaction.setDescription(TextUtils.isEmpty(description) ? "Chuyển khoản" : description);
        transaction.setFromBankName(Constants.BANK_NAME);

        if (isInternalTransfer) {
            transaction.setType(Transaction.TransactionType.TRANSFER_INTERNAL);
            transaction.setToAccountId(recipientAccount.getId());
            transaction.setToBankName(Constants.BANK_NAME);
        } else {
            transaction.setType(Transaction.TransactionType.TRANSFER_EXTERNAL);
            transaction.setToBankName(spinnerBank.getSelectedItem().toString());
        }

        // Check if OTP/Face verification required
        if (transaction.requiresFaceVerification()) {
            // Navigate to face verification
            Intent intent = new Intent(this, FaceVerificationActivity.class);
            intent.putExtra("transaction", transaction);
            if (isInternalTransfer) {
                intent.putExtra("to_account", recipientAccount);
            }
            startActivity(intent);
        } else if (transaction.requires2FA()) {
            // Navigate to OTP verification
            Intent intent = new Intent(this, OTPVerificationActivity.class);
            intent.putExtra("transaction", transaction);
            if (isInternalTransfer) {
                intent.putExtra("to_account", recipientAccount);
            }
            startActivity(intent);
        } else {
            // Process transfer directly
            processTransfer(transaction);
        }
    }

    private void processTransfer(Transaction transaction) {
        showLoading(true);

        if (isInternalTransfer && recipientAccount != null) {
            firebaseHelper.processTransfer(transaction, selectedFromAccount, recipientAccount,
                aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Chuyển khoản thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> {
                    showLoading(false);
                    Toast.makeText(this, "Chuyển khoản thất bại: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            );
        } else {
            // External transfer - just save transaction and update balance
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            firebaseHelper.updateAccountBalance(selectedFromAccount.getId(),
                    selectedFromAccount.getBalance() - transaction.getTotalAmount(),
                task -> {
                    if (task.isSuccessful()) {
                        firebaseHelper.createTransaction(transaction, taskTx -> {
                            showLoading(false);
                            Toast.makeText(this, "Chuyển khoản thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Chuyển khoản thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_QR_SCAN && resultCode == RESULT_OK && data != null) {
            String qrContent = data.getStringExtra("qr_content");
            if (qrContent != null) {
                // Parse QR content and fill form
                etAccountNumber.setText(qrContent);
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnTransfer.setEnabled(!show);
    }
}
