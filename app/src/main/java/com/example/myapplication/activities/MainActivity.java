package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AccountAdapter;
import com.example.myapplication.adapters.TransactionAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvUserName, tvTotalBalance;
    private TextView tvNoAccounts, tvNoTransactions;
    private RecyclerView rvAccounts, rvTransactions;
    private ProgressBar progressBar;

    private View actionTransfer, actionQR, actionBill, actionTopUp;
    private View actionDeposit, actionWithdraw, actionHistory, actionMore;

    private SessionManager sessionManager;
    private FirebaseHelper firebaseHelper;

    private List<Account> accountList = new ArrayList<>();
    private List<Transaction> transactionList = new ArrayList<>();
    private AccountAdapter accountAdapter;
    private TransactionAdapter transactionAdapter;
    private String primaryAccountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initFirebase();
        setupToolbar();
        setupQuickActions();
        setupRecyclerViews();
        setupSwipeRefresh();
        loadUserData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvUserName = findViewById(R.id.tvUserName);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvNoAccounts = findViewById(R.id.tvNoAccounts);
        tvNoTransactions = findViewById(R.id.tvNoTransactions);
        rvAccounts = findViewById(R.id.rvAccounts);
        rvTransactions = findViewById(R.id.rvTransactions);
        progressBar = findViewById(R.id.progressBar);

        actionTransfer = findViewById(R.id.actionTransfer);
        actionQR = findViewById(R.id.actionQR);
        actionBill = findViewById(R.id.actionBill);
        actionTopUp = findViewById(R.id.actionTopUp);
        actionDeposit = findViewById(R.id.actionDeposit);
        actionWithdraw = findViewById(R.id.actionWithdraw);
        actionHistory = findViewById(R.id.actionHistory);
        actionMore = findViewById(R.id.actionMore);

        findViewById(R.id.tvSeeAllTransactions).setOnClickListener(v -> 
            startActivity(new Intent(this, TransactionHistoryActivity.class)));
    }

    private void initFirebase() {
        sessionManager = SessionManager.getInstance(this);
        firebaseHelper = FirebaseHelper.getInstance();

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> showMenuDialog());
    }

    private void showMenuDialog() {
        String[] options = {"Hồ sơ", "Mã QR của tôi", "Mở tài khoản mới", "Tìm chi nhánh", "Đăng xuất"};
        new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: startActivity(new Intent(this, ProfileActivity.class)); break;
                        case 1: startActivity(new Intent(this, QRCodeActivity.class)); break;
                        case 2: startActivity(new Intent(this, OpenAccountActivity.class)); break;
                        case 3: startActivity(new Intent(this, MapActivity.class)); break;
                        case 4: logout(); break;
                    }
                })
                .show();
    }

    private void setupQuickActions() {
        setupAction(actionTransfer, R.drawable.ic_transfer, "Chuyển tiền", 
            v -> startActivity(new Intent(this, TransferActivity.class)));
        setupAction(actionQR, R.drawable.ic_qr, "QR Pay", 
            v -> startActivity(new Intent(this, QRScannerActivity.class)));
        setupAction(actionBill, R.drawable.ic_bill, "Hóa đơn", 
            v -> startActivity(new Intent(this, BillPaymentActivity.class)));
        setupAction(actionTopUp, R.drawable.ic_topup, "Nạp ĐT", 
            v -> startActivity(new Intent(this, PhoneTopUpActivity.class)));
        setupAction(actionDeposit, R.drawable.ic_deposit, "Nạp tiền", 
            v -> openAccountAction("deposit"));
        setupAction(actionWithdraw, R.drawable.ic_withdraw, "Rút tiền", 
            v -> openAccountAction("withdraw"));
        setupAction(actionHistory, R.drawable.ic_history, "Lịch sử", 
            v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
        setupAction(actionMore, R.drawable.ic_add, "Mở TK", 
            v -> startActivity(new Intent(this, OpenAccountActivity.class)));
    }

    private void openAccountAction(String action) {
        if (primaryAccountId != null) {
            Intent intent = new Intent(this, AccountDetailActivity.class);
            intent.putExtra("account_id", primaryAccountId);
            intent.putExtra("action", action);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Chưa có tài khoản", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAction(View view, int iconRes, String label, View.OnClickListener listener) {
        if (view == null) return;
        ImageView icon = view.findViewById(R.id.ivIcon);
        TextView text = view.findViewById(R.id.tvLabel);
        if (icon != null) icon.setImageResource(iconRes);
        if (text != null) text.setText(label);
        view.setOnClickListener(listener);
    }

    private void setupRecyclerViews() {
        rvAccounts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        accountAdapter = new AccountAdapter(this, accountList);
        rvAccounts.setAdapter(accountAdapter);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadUserData() {
        tvUserName.setText(sessionManager.getUserName());
    }

    private void loadData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;
        loadAccounts(userId);
    }

    private void loadAccounts(String userId) {
        firebaseHelper.getUserAccounts(userId,
            querySnapshot -> {
                accountList.clear();
                double totalBalance = 0;
                primaryAccountId = null;

                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null) {
                        account.setId(doc.getId());
                        accountList.add(account);
                        totalBalance += account.getBalance();
                        if (primaryAccountId == null && account.getAccountType() == Account.AccountType.CHECKING) {
                            primaryAccountId = account.getId();
                        }
                    }
                }

                tvTotalBalance.setText(FormatUtils.formatCurrency(totalBalance));
                accountAdapter.notifyDataSetChanged();
                tvNoAccounts.setVisibility(accountList.isEmpty() ? View.VISIBLE : View.GONE);
                rvAccounts.setVisibility(accountList.isEmpty() ? View.GONE : View.VISIBLE);

                if (primaryAccountId != null) {
                    loadTransactions(primaryAccountId);
                } else {
                    swipeRefresh.setRefreshing(false);
                    tvNoTransactions.setVisibility(View.VISIBLE);
                    rvTransactions.setVisibility(View.GONE);
                }
            },
            e -> {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadTransactions(String accountId) {
        firebaseHelper.getAccountTransactionsBoth(accountId, 10,
            transactionListResult -> {
                swipeRefresh.setRefreshing(false);
                transactionList.clear();
                transactionList.addAll(transactionListResult);

                transactionAdapter = new TransactionAdapter(this, transactionList, accountId);
                rvTransactions.setAdapter(transactionAdapter);
                tvNoTransactions.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                rvTransactions.setVisibility(transactionList.isEmpty() ? View.GONE : View.VISIBLE);
            },
            e -> {
                swipeRefresh.setRefreshing(false);
                tvNoTransactions.setVisibility(View.VISIBLE);
            }
        );
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    sessionManager.logout();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
