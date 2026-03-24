package com.example.myapplication.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.TransactionAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private Spinner spinnerAccounts;
    private RecyclerView rvTransactions;
    private TextView tvEmpty, tvTotalIncome, tvTotalExpense, tvTransactionCount;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    
    private List<Account> accounts = new ArrayList<>();
    private List<String> accountNames = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private String selectedAccountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);

        setupToolbar();
        loadAccounts();
    }

    private void initViews() {
        spinnerAccounts = findViewById(R.id.spinnerAccounts);
        rvTransactions = findViewById(R.id.rvTransactions);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
        progressBar = findViewById(R.id.progressBar);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadAccounts() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUserId();

        firebaseHelper.getUserAccounts(userId,
            querySnapshot -> {
                accounts.clear();
                accountNames.clear();

                // Add "All accounts" option
                accountNames.add("Tất cả tài khoản");

                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null) {
                        account.setId(doc.getId());
                        accounts.add(account);

                        String typeName = getAccountTypeName(account.getAccountType());
                        String lastFour = account.getAccountNumber();
                        if (lastFour != null && lastFour.length() >= 4) {
                            lastFour = "****" + lastFour.substring(lastFour.length() - 4);
                        }
                        accountNames.add(typeName + " - " + lastFour);
                    }
                }

                setupSpinner();
                progressBar.setVisibility(View.GONE);

                // Load all transactions initially
                if (!accounts.isEmpty()) {
                    loadAllTransactions();
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Lỗi tải tài khoản", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, accountNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccounts.setAdapter(adapter);

        spinnerAccounts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // All accounts
                    selectedAccountId = null;
                    loadAllTransactions();
                } else {
                    // Specific account
                    Account selected = accounts.get(position - 1);
                    selectedAccountId = selected.getId();
                    loadTransactions(selectedAccountId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAllTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        transactions.clear();
        
        final int[] loadedCount = {0};
        final int totalAccounts = accounts.size();

        if (totalAccounts == 0) {
            progressBar.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        for (Account account : accounts) {
            firebaseHelper.getAccountTransactionsBoth(account.getId(), 50,
                transactionList -> {
                    for (Transaction t : transactionList) {
                        // Avoid duplicates
                        boolean exists = false;
                        for (Transaction existing : transactions) {
                            if (existing.getId() != null && existing.getId().equals(t.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            transactions.add(t);
                        }
                    }

                    loadedCount[0]++;
                    if (loadedCount[0] >= totalAccounts) {
                        // Sort by date descending
                        transactions.sort((t1, t2) -> {
                            if (t1.getCreatedAt() == null || t2.getCreatedAt() == null) return 0;
                            return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                        });
                        updateUI();
                    }
                },
                e -> {
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalAccounts) {
                        updateUI();
                    }
                }
            );
        }
    }

    private void loadTransactions(String accountId) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseHelper.getAccountTransactionsBoth(accountId, 100,
            transactionList -> {
                transactions.clear();
                transactions.addAll(transactionList);
                updateUI();
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Lỗi tải giao dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void updateUI() {
        progressBar.setVisibility(View.GONE);

        if (transactions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);

            String accountIdForAdapter = selectedAccountId;
            if (accountIdForAdapter == null && !accounts.isEmpty()) {
                accountIdForAdapter = accounts.get(0).getId();
            }

            TransactionAdapter adapter = new TransactionAdapter(this, transactions, accountIdForAdapter);
            rvTransactions.setAdapter(adapter);
        }

        // Calculate summary
        calculateSummary();
    }

    private void calculateSummary() {
        double totalIncome = 0;
        double totalExpense = 0;

        // Get all user's account IDs for comparison
        java.util.Set<String> userAccountIds = new java.util.HashSet<>();
        for (Account acc : accounts) {
            if (acc.getId() != null) {
                userAccountIds.add(acc.getId());
            }
        }

        for (Transaction t : transactions) {
            boolean isIncoming = false;
            
            // Determine if transaction is incoming or outgoing
            switch (t.getType()) {
                case DEPOSIT:
                    // Deposit is always incoming
                    isIncoming = true;
                    break;
                    
                case WITHDRAWAL:
                    // Withdrawal is always outgoing
                    isIncoming = false;
                    break;
                    
                case TRANSFER:
                    // For transfers, check the direction
                    if (selectedAccountId != null) {
                        // Specific account selected
                        // Incoming if this account is the recipient (toAccountId)
                        isIncoming = selectedAccountId.equals(t.getToAccountId());
                    } else {
                        // All accounts selected
                        // Incoming if toAccountId is one of user's accounts AND fromAccountId is NOT user's account
                        // (external transfer in)
                        // Outgoing if fromAccountId is one of user's accounts
                        boolean fromUserAccount = t.getFromAccountId() != null && 
                                userAccountIds.contains(t.getFromAccountId());
                        boolean toUserAccount = t.getToAccountId() != null && 
                                userAccountIds.contains(t.getToAccountId());
                        
                        if (fromUserAccount && toUserAccount) {
                            // Internal transfer between user's own accounts - don't count
                            // Skip this transaction in summary
                            continue;
                        } else if (toUserAccount && !fromUserAccount) {
                            // External transfer INTO user's account
                            isIncoming = true;
                        } else {
                            // Transfer FROM user's account (outgoing)
                            isIncoming = false;
                        }
                    }
                    break;
                    
                case BILL_PAYMENT:
                case PHONE_TOPUP:
                case LOAN_PAYMENT:
                    // These are always outgoing expenses
                    isIncoming = false;
                    break;
                    
                default:
                    // Default to expense
                    isIncoming = false;
                    break;
            }

            if (isIncoming) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
            }
        }

        tvTotalIncome.setText("+" + FormatUtils.formatCurrency(totalIncome));
        tvTotalExpense.setText("-" + FormatUtils.formatCurrency(totalExpense));
        tvTransactionCount.setText(String.valueOf(transactions.size()));
    }

    private String getAccountTypeName(Account.AccountType type) {
        if (type == null) return "Tài khoản";
        switch (type) {
            case CHECKING: return "Thanh toán";
            case SAVING: return "Tiết kiệm";
            case MORTGAGE: return "Vay";
            default: return "Tài khoản";
        }
    }
}
