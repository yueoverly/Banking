package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.example.myapplication.R;
import com.example.myapplication.adapters.TransactionAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display account details and transaction history
 */
public class AccountDetailActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private MaterialCardView cardAccountInfo;
    private ImageView ivAccountIcon;
    private TextView tvAccountType, tvAccountNumber, tvAccountBalance;
    private TextView tvAccountStatus, tvInterestRate, tvMonthlyProfit;
    private TextView tvLoanAmount, tvMonthlyPayment, tvRemainingTerm;
    private LinearLayout layoutSavingDetails, layoutMortgageDetails;
    private MaterialButton btnTransfer, btnDeposit, btnWithdraw;
    private RecyclerView rvTransactions;
    private TextView tvNoTransactions;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    
    private String accountId;
    private Account currentAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        accountId = getIntent().getStringExtra("account_id");
        if (accountId == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadAccountData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        cardAccountInfo = findViewById(R.id.card_account_info);
        ivAccountIcon = findViewById(R.id.iv_account_icon);
        tvAccountType = findViewById(R.id.tv_account_type);
        tvAccountNumber = findViewById(R.id.tv_account_number);
        tvAccountBalance = findViewById(R.id.tv_account_balance);
        tvAccountStatus = findViewById(R.id.tv_account_status);
        
        tvInterestRate = findViewById(R.id.tv_interest_rate);
        tvMonthlyProfit = findViewById(R.id.tv_monthly_profit);
        layoutSavingDetails = findViewById(R.id.layout_saving_details);
        
        tvLoanAmount = findViewById(R.id.tv_loan_amount);
        tvMonthlyPayment = findViewById(R.id.tv_monthly_payment);
        tvRemainingTerm = findViewById(R.id.tv_remaining_term);
        layoutMortgageDetails = findViewById(R.id.layout_mortgage_details);
        
        btnTransfer = findViewById(R.id.btn_transfer);
        btnDeposit = findViewById(R.id.btn_deposit);
        btnWithdraw = findViewById(R.id.btn_withdraw);
        
        rvTransactions = findViewById(R.id.rv_transactions);
        tvNoTransactions = findViewById(R.id.tv_no_transactions);
        progressBar = findViewById(R.id.progress_bar);

        firebaseHelper = FirebaseHelper.getInstance();

        swipeRefresh.setOnRefreshListener(this::loadAccountData);
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark);

        // Button click listeners
        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferActivity.class);
            intent.putExtra("from_account_id", accountId);
            startActivity(intent);
        });

        btnDeposit.setOnClickListener(v -> {
            showDepositDialog();
        });

        btnWithdraw.setOnClickListener(v -> {
            showWithdrawDialog();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết tài khoản");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList, accountId);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void loadAccountData() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseHelper.getAccount(accountId,
                documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentAccount = documentSnapshot.toObject(Account.class);
                        if (currentAccount != null) {
                            currentAccount.setId(documentSnapshot.getId());
                            displayAccountInfo();
                            loadTransactions();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(this, "Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void displayAccountInfo() {
        // Account type and icon
        String typeName;
        int iconRes;
        int cardColor;
        
        switch (currentAccount.getAccountType()) {
            case SAVING:
                typeName = "Tài khoản Tiết kiệm";
                iconRes = R.drawable.ic_saving;
                cardColor = getResources().getColor(R.color.saving_card);
                layoutSavingDetails.setVisibility(View.VISIBLE);
                layoutMortgageDetails.setVisibility(View.GONE);
                tvInterestRate.setText(FormatUtils.formatInterestRate(currentAccount.getInterestRate()));
                tvMonthlyProfit.setText(FormatUtils.formatCurrency(currentAccount.getMonthlyProfit()));
                btnTransfer.setVisibility(View.GONE);
                break;
                
            case MORTGAGE:
                typeName = "Tài khoản Vay";
                iconRes = R.drawable.ic_mortgage;
                cardColor = getResources().getColor(R.color.mortgage_card);
                layoutSavingDetails.setVisibility(View.GONE);
                layoutMortgageDetails.setVisibility(View.VISIBLE);
                tvLoanAmount.setText(FormatUtils.formatCurrency(currentAccount.getLoanAmount()));
                tvMonthlyPayment.setText(FormatUtils.formatCurrency(currentAccount.getMonthlyPayment()));
                tvRemainingTerm.setText(currentAccount.getTermMonths() + " tháng");
                btnTransfer.setVisibility(View.GONE);
                btnWithdraw.setVisibility(View.GONE);
                break;
                
            case CHECKING:
            default:
                typeName = "Tài khoản Thanh toán";
                iconRes = R.drawable.ic_checking;
                cardColor = getResources().getColor(R.color.checking_card);
                layoutSavingDetails.setVisibility(View.GONE);
                layoutMortgageDetails.setVisibility(View.GONE);
                break;
        }

        tvAccountType.setText(typeName);
        ivAccountIcon.setImageResource(iconRes);
        cardAccountInfo.setCardBackgroundColor(cardColor);
        
        // Account number
        tvAccountNumber.setText(FormatUtils.formatAccountNumber(currentAccount.getAccountNumber()));
        
        // Balance
        tvAccountBalance.setText(FormatUtils.formatCurrency(currentAccount.getBalance()));
        
        // Status
        String statusText;
        int statusColor;
        switch (currentAccount.getStatus()) {
            case ACTIVE:
                statusText = "Hoạt động";
                statusColor = getResources().getColor(R.color.status_active);
                break;
            case INACTIVE:
                statusText = "Không hoạt động";
                statusColor = getResources().getColor(R.color.status_inactive);
                break;
            case FROZEN:
                statusText = "Đã đóng băng";
                statusColor = getResources().getColor(R.color.status_frozen);
                break;
            case CLOSED:
                statusText = "Đã đóng";
                statusColor = getResources().getColor(R.color.status_closed);
                break;
            default:
                statusText = "Không xác định";
                statusColor = getResources().getColor(R.color.text_secondary);
        }
        tvAccountStatus.setText(statusText);
        tvAccountStatus.setTextColor(statusColor);
    }

    private void loadTransactions() {
        firebaseHelper.getAccountTransactions(accountId, 20,
                querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    transactionList.clear();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactionList.add(transaction);
                        }
                    }
                    
                    transactionAdapter.notifyDataSetChanged();
                    
                    if (transactionList.isEmpty()) {
                        tvNoTransactions.setVisibility(View.VISIBLE);
                        rvTransactions.setVisibility(View.GONE);
                    } else {
                        tvNoTransactions.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.VISIBLE);
                    }
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Lỗi tải giao dịch", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void showDepositDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Nạp tiền");

        View view = getLayoutInflater().inflate(R.layout.dialog_amount_input, null);
        final TextView etAmount = view.findViewById(R.id.et_amount);
        
        builder.setView(view);
        builder.setPositiveButton("Nạp tiền", (dialog, which) -> {
            String amountStr = etAmount.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr.replace(",", ""));
                    if (amount >= 10000) {
                        processDeposit(amount);
                    } else {
                        Toast.makeText(this, "Số tiền tối thiểu là 10,000 VND", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showWithdrawDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Rút tiền");

        View view = getLayoutInflater().inflate(R.layout.dialog_amount_input, null);
        final TextView etAmount = view.findViewById(R.id.et_amount);
        
        builder.setView(view);
        builder.setPositiveButton("Rút tiền", (dialog, which) -> {
            String amountStr = etAmount.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr.replace(",", ""));
                    if (amount >= 10000) {
                        if (amount <= currentAccount.getBalance()) {
                            processWithdraw(amount);
                        } else {
                            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Số tiền tối thiểu là 10,000 VND", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void processDeposit(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        
        double newBalance = currentAccount.getBalance() + amount;
        firebaseHelper.updateAccountBalance(accountId, newBalance,
                aVoid -> {
                    // Create transaction record
                    Transaction transaction = new Transaction();
                    transaction.setToAccountId(accountId);
                    transaction.setAmount(amount);
                    transaction.setType(Transaction.TransactionType.DEPOSIT);
                    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                    transaction.setDescription("Nạp tiền vào tài khoản");
                    
                    firebaseHelper.createTransaction(transaction,
                            docRef -> {
                                Toast.makeText(this, "Nạp tiền thành công", Toast.LENGTH_SHORT).show();
                                loadAccountData();
                            },
                            e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi tạo giao dịch", Toast.LENGTH_SHORT).show();
                            }
                    );
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi nạp tiền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void processWithdraw(double amount) {
        progressBar.setVisibility(View.VISIBLE);
        
        double newBalance = currentAccount.getBalance() - amount;
        firebaseHelper.updateAccountBalance(accountId, newBalance,
                aVoid -> {
                    // Create transaction record
                    Transaction transaction = new Transaction();
                    transaction.setFromAccountId(accountId);
                    transaction.setAmount(amount);
                    transaction.setType(Transaction.TransactionType.WITHDRAWAL);
                    transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                    transaction.setDescription("Rút tiền từ tài khoản");
                    
                    firebaseHelper.createTransaction(transaction,
                            docRef -> {
                                Toast.makeText(this, "Rút tiền thành công", Toast.LENGTH_SHORT).show();
                                loadAccountData();
                            },
                            e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Lỗi tạo giao dịch", Toast.LENGTH_SHORT).show();
                            }
                    );
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi rút tiền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accountId != null) {
            loadAccountData();
        }
    }
}
