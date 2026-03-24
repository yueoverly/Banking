package com.example.myapplication.activities;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AccountAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CustomerDetailActivity - Cho nhân viên xem chi tiết và tạo tài khoản cho khách hàng
 * Đặc biệt: Có thể tạo TK Vay thế chấp (Mortgage) cho khách hàng
 */
public class CustomerDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    private TextView tvCustomerName, tvCustomerEmail, tvCustomerPhone, tvIdCard;
    private TextView tvNoAccounts;
    private RecyclerView rvAccounts;
    private Button btnCreateChecking, btnCreateSaving, btnCreateMortgage;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private String customerId;
    private User customer;
    private List<Account> accounts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        customerId = getIntent().getStringExtra(EXTRA_CUSTOMER_ID);
        if (customerId == null) {
            finish();
            return;
        }

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        setupToolbar();
        setupListeners();
        loadCustomerData();
    }

    private void initViews() {
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        tvIdCard = findViewById(R.id.tvIdCard);
        tvNoAccounts = findViewById(R.id.tvNoAccounts);
        rvAccounts = findViewById(R.id.rvAccounts);
        btnCreateChecking = findViewById(R.id.btnCreateChecking);
        btnCreateSaving = findViewById(R.id.btnCreateSaving);
        btnCreateMortgage = findViewById(R.id.btnCreateMortgage);
        progressBar = findViewById(R.id.progressBar);

        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnCreateChecking.setOnClickListener(v -> showCreateAccountDialog(Account.AccountType.CHECKING));
        btnCreateSaving.setOnClickListener(v -> showCreateAccountDialog(Account.AccountType.SAVING));
        btnCreateMortgage.setOnClickListener(v -> showCreateMortgageDialog());
    }

    private void loadCustomerData() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseHelper.getUser(customerId,
            documentSnapshot -> {
                customer = documentSnapshot.toObject(User.class);
                if (customer != null) {
                    customer.setId(documentSnapshot.getId());
                    displayCustomerInfo();
                    loadAccounts();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tìm thấy khách hàng", Toast.LENGTH_SHORT).show();
                }
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void displayCustomerInfo() {
        tvCustomerName.setText(customer.getFullName());
        tvCustomerEmail.setText(customer.getEmail());
        tvCustomerPhone.setText(customer.getPhone());
        tvIdCard.setText("CMND/CCCD: " + (customer.getIdCardNumber() != null ? customer.getIdCardNumber() : "N/A"));
    }

    private void loadAccounts() {
        firebaseHelper.getUserAccounts(customerId,
            querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                accounts.clear();

                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null) {
                        account.setId(doc.getId());
                        accounts.add(account);
                    }
                }

                updateAccountsList();
                updateCreateButtons();
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải tài khoản", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void updateAccountsList() {
        if (accounts.isEmpty()) {
            tvNoAccounts.setVisibility(View.VISIBLE);
            rvAccounts.setVisibility(View.GONE);
        } else {
            tvNoAccounts.setVisibility(View.GONE);
            rvAccounts.setVisibility(View.VISIBLE);
            AccountAdapter adapter = new AccountAdapter(this, accounts);
            rvAccounts.setAdapter(adapter);
        }
    }

    private void updateCreateButtons() {
        boolean hasChecking = false, hasSaving = false, hasMortgage = false;

        for (Account account : accounts) {
            switch (account.getAccountType()) {
                case CHECKING: hasChecking = true; break;
                case SAVING: hasSaving = true; break;
                case MORTGAGE: hasMortgage = true; break;
            }
        }

        btnCreateChecking.setEnabled(!hasChecking);
        btnCreateChecking.setAlpha(hasChecking ? 0.5f : 1f);
        if (hasChecking) btnCreateChecking.setText("✓ Thanh toán");

        btnCreateSaving.setEnabled(!hasSaving);
        btnCreateSaving.setAlpha(hasSaving ? 0.5f : 1f);
        if (hasSaving) btnCreateSaving.setText("✓ Tiết kiệm");

        btnCreateMortgage.setEnabled(!hasMortgage);
        btnCreateMortgage.setAlpha(hasMortgage ? 0.5f : 1f);
        if (hasMortgage) btnCreateMortgage.setText("✓ Vay");
    }

    private void showCreateAccountDialog(Account.AccountType type) {
        String typeName = type == Account.AccountType.CHECKING ? "Thanh toán" : "Tiết kiệm";
        double defaultRate = type == Account.AccountType.SAVING ? 5.5 : 0;

        EditText input = new EditText(this);
        input.setHint("Số tiền ban đầu");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Tạo tài khoản " + typeName)
                .setMessage("Khách hàng: " + customer.getFullName() +
                           (type == Account.AccountType.SAVING ? "\nLãi suất: 5.5%/năm" : ""))
                .setView(input)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    try {
                        double balance = Double.parseDouble(input.getText().toString());
                        createAccount(type, balance, defaultRate, 0, 0);
                    } catch (NumberFormatException e) {
                        createAccount(type, 0, defaultRate, 0, 0);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCreateMortgageDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etLoanAmount = new EditText(this);
        etLoanAmount.setHint("Số tiền vay (VND)");
        etLoanAmount.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etLoanAmount);

        EditText etLoanTerm = new EditText(this);
        etLoanTerm.setHint("Kỳ hạn vay (tháng)");
        etLoanTerm.setInputType(InputType.TYPE_CLASS_NUMBER);
        etLoanTerm.setText("120"); // Default 10 years
        layout.addView(etLoanTerm);

        EditText etInterestRate = new EditText(this);
        etInterestRate.setHint("Lãi suất (%/năm)");
        etInterestRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etInterestRate.setText("8.0");
        layout.addView(etInterestRate);

        new AlertDialog.Builder(this)
                .setTitle("🏠 Tạo tài khoản Vay thế chấp")
                .setMessage("Khách hàng: " + customer.getFullName())
                .setView(layout)
                .setPositiveButton("Tạo khoản vay", (dialog, which) -> {
                    try {
                        double loanAmount = Double.parseDouble(etLoanAmount.getText().toString());
                        int loanTerm = Integer.parseInt(etLoanTerm.getText().toString());
                        double interestRate = Double.parseDouble(etInterestRate.getText().toString());

                        if (loanAmount < 10000000) {
                            Toast.makeText(this, "Số tiền vay tối thiểu 10,000,000đ", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (loanTerm < 6) {
                            Toast.makeText(this, "Kỳ hạn tối thiểu 6 tháng", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        createAccount(Account.AccountType.MORTGAGE, 0, interestRate, loanAmount, loanTerm);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Dữ liệu không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createAccount(Account.AccountType type, double balance, double interestRate, 
                               double loanAmount, int loanTermMonths) {
        progressBar.setVisibility(View.VISIBLE);

        Account account = new Account(customerId, type);
        account.setBalance(balance);
        account.setInterestRate(interestRate);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());

        if (type == Account.AccountType.MORTGAGE) {
            account.setLoanAmount(loanAmount);
            account.setLoanTermMonths(loanTermMonths);
            account.setRemainingDebt(loanAmount);
        }

        firebaseHelper.createAccount(account, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                String typeName;
                switch (type) {
                    case CHECKING: typeName = "Thanh toán"; break;
                    case SAVING: typeName = "Tiết kiệm"; break;
                    case MORTGAGE: typeName = "Vay thế chấp"; break;
                    default: typeName = "Tài khoản"; break;
                }

                String message = "✅ Đã tạo tài khoản " + typeName + "!\n\n" +
                        "Số TK: " + account.getFormattedAccountNumber();
                
                if (type == Account.AccountType.MORTGAGE) {
                    double monthlyPayment = account.getMonthlyPayment();
                    message += "\nSố tiền vay: " + FormatUtils.formatCurrency(loanAmount) +
                              "\nKỳ hạn: " + loanTermMonths + " tháng" +
                              "\nLãi suất: " + interestRate + "%/năm" +
                              "\nTrả hàng tháng: " + FormatUtils.formatCurrency(monthlyPayment);
                }

                new AlertDialog.Builder(this)
                        .setTitle("Thành công")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();

                loadAccounts(); // Refresh list
            } else {
                Toast.makeText(this, "Lỗi tạo tài khoản", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
