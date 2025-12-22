package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.Constants;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for phone top-up
 */
public class PhoneTopUpActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText etPhoneNumber;
    private Spinner spinnerProvider, spinnerAccount;
    private RadioGroup radioGroupAmount;
    private RadioButton rb10k, rb20k, rb50k, rb100k, rb200k, rb500k;
    private EditText etCustomAmount;
    private TextView tvAccountBalance, tvSelectedAmount;
    private MaterialButton btnTopUp;
    private MaterialCardView cardProviders;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;

    private List<Account> accountList;
    private Account selectedAccount;
    private double selectedAmount = 0;

    private String[] providers = {"Viettel", "Vinaphone", "Mobifone", "Vietnamobile", "Gmobile"};
    private double[] presetAmounts = {10000, 20000, 50000, 100000, 200000, 500000};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_topup);

        initViews();
        setupToolbar();
        setupSpinners();
        setupAmountSelection();
        loadAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        spinnerProvider = findViewById(R.id.spinner_provider);
        spinnerAccount = findViewById(R.id.spinner_account);
        radioGroupAmount = findViewById(R.id.radio_group_amount);
        rb10k = findViewById(R.id.rb_10k);
        rb20k = findViewById(R.id.rb_20k);
        rb50k = findViewById(R.id.rb_50k);
        rb100k = findViewById(R.id.rb_100k);
        rb200k = findViewById(R.id.rb_200k);
        rb500k = findViewById(R.id.rb_500k);
        etCustomAmount = findViewById(R.id.et_custom_amount);
        tvAccountBalance = findViewById(R.id.tv_account_balance);
        tvSelectedAmount = findViewById(R.id.tv_selected_amount);
        btnTopUp = findViewById(R.id.btn_top_up);
        cardProviders = findViewById(R.id.card_providers);
        progressBar = findViewById(R.id.progress_bar);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        accountList = new ArrayList<>();

        btnTopUp.setOnClickListener(v -> processTopUp());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nạp tiền điện thoại");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        // Provider spinner
        ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, providers);
        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvider.setAdapter(providerAdapter);

        // Auto-detect provider from phone number
        etPhoneNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                autoDetectProvider(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void autoDetectProvider(String phoneNumber) {
        if (phoneNumber.length() >= 3) {
            String prefix = phoneNumber.startsWith("0") ? phoneNumber.substring(1, Math.min(3, phoneNumber.length())) : phoneNumber.substring(0, Math.min(2, phoneNumber.length()));
            
            // Viettel prefixes
            if (prefix.matches("(86|96|97|98|32|33|34|35|36|37|38|39).*")) {
                spinnerProvider.setSelection(0);
            }
            // Vinaphone prefixes
            else if (prefix.matches("(88|91|94|81|82|83|84|85).*")) {
                spinnerProvider.setSelection(1);
            }
            // Mobifone prefixes
            else if (prefix.matches("(89|90|93|70|76|77|78|79).*")) {
                spinnerProvider.setSelection(2);
            }
            // Vietnamobile
            else if (prefix.matches("(92|56|58).*")) {
                spinnerProvider.setSelection(3);
            }
            // Gmobile
            else if (prefix.matches("(99|59).*")) {
                spinnerProvider.setSelection(4);
            }
        }
    }

    private void setupAmountSelection() {
        radioGroupAmount.setOnCheckedChangeListener((group, checkedId) -> {
            etCustomAmount.setText("");
            
            if (checkedId == R.id.rb_10k) {
                selectedAmount = 10000;
            } else if (checkedId == R.id.rb_20k) {
                selectedAmount = 20000;
            } else if (checkedId == R.id.rb_50k) {
                selectedAmount = 50000;
            } else if (checkedId == R.id.rb_100k) {
                selectedAmount = 100000;
            } else if (checkedId == R.id.rb_200k) {
                selectedAmount = 200000;
            } else if (checkedId == R.id.rb_500k) {
                selectedAmount = 500000;
            }
            
            updateSelectedAmount();
        });

        etCustomAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    radioGroupAmount.clearCheck();
                    try {
                        selectedAmount = Double.parseDouble(s.toString().replace(",", ""));
                    } catch (NumberFormatException e) {
                        selectedAmount = 0;
                    }
                    updateSelectedAmount();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void updateSelectedAmount() {
        if (selectedAmount > 0) {
            tvSelectedAmount.setText("Số tiền nạp: " + FormatUtils.formatCurrency(selectedAmount));
            tvSelectedAmount.setVisibility(View.VISIBLE);
        } else {
            tvSelectedAmount.setVisibility(View.GONE);
        }
    }

    private void loadAccounts() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        firebaseHelper.getUserAccounts(userId,
                querySnapshot -> {
                    accountList.clear();
                    List<String> accountNames = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Account account = doc.toObject(Account.class);
                        if (account != null && account.getAccountType() == Account.AccountType.CHECKING) {
                            account.setId(doc.getId());
                            accountList.add(account);
                            accountNames.add(FormatUtils.maskAccountNumber(account.getAccountNumber())
                                    + " - " + FormatUtils.formatCurrency(account.getBalance()));
                        }
                    }

                    ArrayAdapter<String> accountAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, accountNames);
                    accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAccount.setAdapter(accountAdapter);

                    spinnerAccount.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                            if (position < accountList.size()) {
                                selectedAccount = accountList.get(position);
                                tvAccountBalance.setText("Số dư: " + FormatUtils.formatCurrency(selectedAccount.getBalance()));
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });

                    if (!accountList.isEmpty()) {
                        selectedAccount = accountList.get(0);
                        tvAccountBalance.setText("Số dư: " + FormatUtils.formatCurrency(selectedAccount.getBalance()));
                    }
                },
                e -> Toast.makeText(this, "Lỗi tải danh sách tài khoản", Toast.LENGTH_SHORT).show()
        );
    }

    private void processTopUp() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        
        // Validate phone number
        if (phoneNumber.isEmpty()) {
            etPhoneNumber.setError("Vui lòng nhập số điện thoại");
            return;
        }
        
        if (!phoneNumber.matches("^(0|\\+84)[0-9]{9,10}$")) {
            etPhoneNumber.setError("Số điện thoại không hợp lệ");
            return;
        }

        if (selectedAccount == null) {
            Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAmount <= 0) {
            Toast.makeText(this, "Vui lòng chọn mệnh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAmount < 10000) {
            Toast.makeText(this, "Mệnh giá tối thiểu là 10,000 VND", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedAmount > selectedAccount.getBalance()) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if OTP required
        if (selectedAmount >= Constants.OTP_REQUIRED_THRESHOLD) {
            Intent intent = new Intent(this, OTPVerificationActivity.class);
            intent.putExtra("from_account_id", selectedAccount.getId());
            intent.putExtra("amount", selectedAmount);
            intent.putExtra("fee", 0.0);
            intent.putExtra("transaction_type", "phone_topup");
            intent.putExtra("description", "Nạp tiền " + providers[spinnerProvider.getSelectedItemPosition()] 
                    + " - " + phoneNumber);
            startActivityForResult(intent, 100);
        } else {
            executeTopUp(phoneNumber);
        }
    }

    private void executeTopUp(String phoneNumber) {
        progressBar.setVisibility(View.VISIBLE);
        btnTopUp.setEnabled(false);

        // Update account balance
        double newBalance = selectedAccount.getBalance() - selectedAmount;
        firebaseHelper.updateAccountBalance(selectedAccount.getId(), newBalance,
                aVoid -> {
                    // Create transaction record
                    Transaction transaction = new Transaction();
                    transaction.setFromAccountId(selectedAccount.getId());
                    transaction.setAmount(selectedAmount);
                    transaction.setType(Transaction.TransactionType.PHONE_TOPUP);
                    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                    transaction.setDescription("Nạp tiền " + providers[spinnerProvider.getSelectedItemPosition()]
                            + " - " + phoneNumber);

                    firebaseHelper.createTransaction(transaction,
                            docRef -> {
                                progressBar.setVisibility(View.GONE);
                                showSuccessDialog(phoneNumber);
                            },
                            e -> {
                                progressBar.setVisibility(View.GONE);
                                btnTopUp.setEnabled(true);
                                Toast.makeText(this, "Lỗi tạo giao dịch", Toast.LENGTH_SHORT).show();
                            }
                    );
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnTopUp.setEnabled(true);
                    Toast.makeText(this, "Lỗi nạp tiền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showSuccessDialog(String phoneNumber) {
        String message = String.format("Nạp %s cho số %s thành công!",
                FormatUtils.formatCurrency(selectedAmount), phoneNumber);
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("Thành công")
                .setMessage(message)
                .setPositiveButton("Đóng", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            String phoneNumber = etPhoneNumber.getText().toString().trim();
            showSuccessDialog(phoneNumber);
        }
    }
}
