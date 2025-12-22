package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.example.myapplication.models.Bill;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.Constants;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for paying utility bills (electricity, water, etc.)
 */
public class BillPaymentActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Spinner spinnerBillType, spinnerProvider, spinnerAccount;
    private EditText etCustomerCode, etAmount;
    private TextView tvBillInfo, tvAccountBalance, tvFee, tvTotal;
    private MaterialButton btnLookup, btnPay;
    private MaterialCardView cardBillInfo;
    private LinearLayout layoutBillDetails;
    private ProgressBar progressBar;
    private ImageView ivBillIcon;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;

    private List<Account> accountList;
    private Account selectedAccount;
    private Bill currentBill;

    private String[] billTypes = {"Điện", "Nước", "Internet", "Điện thoại cố định", "Truyền hình", "Bảo hiểm"};
    private String[][] providers = {
            {"EVN Miền Nam", "EVN Miền Bắc", "EVN Miền Trung"},
            {"Sawaco", "Nước Hà Nội", "Nước Đà Nẵng"},
            {"VNPT", "Viettel", "FPT"},
            {"VNPT", "Viettel"},
            {"VTVcab", "SCTV", "K+"},
            {"Bảo Việt", "Prudential", "Manulife"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_payment);

        initViews();
        setupToolbar();
        setupSpinners();
        loadAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerBillType = findViewById(R.id.spinner_bill_type);
        spinnerProvider = findViewById(R.id.spinner_provider);
        spinnerAccount = findViewById(R.id.spinner_account);
        etCustomerCode = findViewById(R.id.et_customer_code);
        etAmount = findViewById(R.id.et_amount);
        tvBillInfo = findViewById(R.id.tv_bill_info);
        tvAccountBalance = findViewById(R.id.tv_account_balance);
        tvFee = findViewById(R.id.tv_fee);
        tvTotal = findViewById(R.id.tv_total);
        btnLookup = findViewById(R.id.btn_lookup);
        btnPay = findViewById(R.id.btn_pay);
        cardBillInfo = findViewById(R.id.card_bill_info);
        layoutBillDetails = findViewById(R.id.layout_bill_details);
        progressBar = findViewById(R.id.progress_bar);
        ivBillIcon = findViewById(R.id.iv_bill_icon);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        accountList = new ArrayList<>();

        // Setup listeners
        btnLookup.setOnClickListener(v -> lookupBill());
        btnPay.setOnClickListener(v -> processBillPayment());

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán hóa đơn");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSpinners() {
        // Bill type spinner
        ArrayAdapter<String> billTypeAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, billTypes);
        billTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBillType.setAdapter(billTypeAdapter);

        spinnerBillType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateProviders(position);
                updateBillIcon(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Initialize providers for first bill type
        updateProviders(0);
    }

    private void updateProviders(int billTypeIndex) {
        if (billTypeIndex < providers.length) {
            ArrayAdapter<String> providerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, providers[billTypeIndex]);
            providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProvider.setAdapter(providerAdapter);
        }
    }

    private void updateBillIcon(int position) {
        int iconRes;
        switch (position) {
            case 0:
                iconRes = R.drawable.ic_electricity;
                break;
            case 1:
                iconRes = R.drawable.ic_water;
                break;
            case 2:
                iconRes = R.drawable.ic_internet;
                break;
            case 3:
                iconRes = R.drawable.ic_phone;
                break;
            case 4:
                iconRes = R.drawable.ic_tv;
                break;
            case 5:
                iconRes = R.drawable.ic_insurance;
                break;
            default:
                iconRes = R.drawable.ic_bill;
        }
        ivBillIcon.setImageResource(iconRes);
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

    private void lookupBill() {
        String customerCode = etCustomerCode.getText().toString().trim();
        if (customerCode.isEmpty()) {
            etCustomerCode.setError("Vui lòng nhập mã khách hàng");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLookup.setEnabled(false);

        // Simulate bill lookup (in production, call actual API)
        new android.os.Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            btnLookup.setEnabled(true);

            // Generate sample bill data
            int billTypeIndex = spinnerBillType.getSelectedItemPosition();
            String billTypeName = billTypes[billTypeIndex];
            String provider = spinnerProvider.getSelectedItem().toString();
            
            // Random amount between 100,000 and 2,000,000
            double amount = 100000 + Math.random() * 1900000;
            amount = Math.round(amount / 1000) * 1000; // Round to nearest 1000

            currentBill = new Bill();
            currentBill.setUserId(sessionManager.getUserId());
            currentBill.setBillType(Bill.BillType.values()[billTypeIndex]);
            currentBill.setProviderCode(provider);
            currentBill.setCustomerCode(customerCode);
            currentBill.setAmount(amount);
            currentBill.setStatus(Bill.BillStatus.UNPAID);

            // Display bill info
            cardBillInfo.setVisibility(View.VISIBLE);
            layoutBillDetails.setVisibility(View.VISIBLE);
            
            String billInfo = String.format(
                    "Loại: %s\nNhà cung cấp: %s\nMã KH: %s\nSố tiền: %s",
                    billTypeName, provider, customerCode, FormatUtils.formatCurrency(amount)
            );
            tvBillInfo.setText(billInfo);
            etAmount.setText(String.format("%.0f", amount));
            
            calculateTotal();
            btnPay.setEnabled(true);

        }, 1500);
    }

    private void calculateTotal() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            tvFee.setText("0 VND");
            tvTotal.setText("0 VND");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr.replace(",", ""));
            double fee = 0; // No fee for bill payment
            double total = amount + fee;

            tvFee.setText(FormatUtils.formatCurrency(fee));
            tvTotal.setText(FormatUtils.formatCurrency(total));
        } catch (NumberFormatException e) {
            tvFee.setText("0 VND");
            tvTotal.setText("0 VND");
        }
    }

    private void processBillPayment() {
        if (selectedAccount == null) {
            Toast.makeText(this, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Vui lòng nhập số tiền");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", ""));
        } catch (NumberFormatException e) {
            etAmount.setError("Số tiền không hợp lệ");
            return;
        }

        if (amount < 10000) {
            etAmount.setError("Số tiền tối thiểu là 10,000 VND");
            return;
        }

        if (amount > selectedAccount.getBalance()) {
            Toast.makeText(this, "Số dư không đủ để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if 2FA required
        if (amount >= Constants.OTP_REQUIRED_THRESHOLD) {
            Intent intent = new Intent(this, OTPVerificationActivity.class);
            intent.putExtra("from_account_id", selectedAccount.getId());
            intent.putExtra("amount", amount);
            intent.putExtra("fee", 0.0);
            intent.putExtra("transaction_type", "bill_payment");
            intent.putExtra("description", "Thanh toán " + billTypes[spinnerBillType.getSelectedItemPosition()]);
            startActivityForResult(intent, 100);
        } else {
            executePayment(amount);
        }
    }

    private void executePayment(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);

        // Update account balance
        double newBalance = selectedAccount.getBalance() - amount;
        firebaseHelper.updateAccountBalance(selectedAccount.getId(), newBalance,
                aVoid -> {
                    // Create transaction record
                    Transaction transaction = new Transaction();
                    transaction.setFromAccountId(selectedAccount.getId());
                    transaction.setAmount(amount);
                    transaction.setType(Transaction.TransactionType.BILL_PAYMENT);
                    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                    transaction.setDescription("Thanh toán " + billTypes[spinnerBillType.getSelectedItemPosition()] 
                            + " - " + spinnerProvider.getSelectedItem().toString());

                    firebaseHelper.createTransaction(transaction,
                            docRef -> {
                                progressBar.setVisibility(View.GONE);
                                showSuccessDialog();
                            },
                            e -> {
                                progressBar.setVisibility(View.GONE);
                                btnPay.setEnabled(true);
                                Toast.makeText(this, "Lỗi tạo giao dịch", Toast.LENGTH_SHORT).show();
                            }
                    );
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnPay.setEnabled(true);
                    Toast.makeText(this, "Lỗi thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showSuccessDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Thành công")
                .setMessage("Thanh toán hóa đơn thành công!")
                .setPositiveButton("Đóng", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // OTP verified, payment completed
            showSuccessDialog();
        }
    }
}
