package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.myapplication.R;
import com.example.myapplication.adapters.AccountAdapter;
import com.example.myapplication.adapters.TransactionAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for customer interface
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;

    // Header views
    private TextView tvGreeting, tvUserName, tvTotalBalance;
    private ImageView imgProfile;

    // Quick action cards
    private CardView cardTransfer, cardQrPay, cardBillPay, cardPhoneTopUp;

    // Account list
    private RecyclerView rvAccounts;
    private AccountAdapter accountAdapter;
    private List<Account> accountList;

    // Recent transactions
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private TextView tvViewAllTransactions;

    // Services
    private LinearLayout llServices;
    private CardView cardElectricity, cardWater, cardTicket, cardMore;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initFirebase();
        setupToolbar();
        setupNavigation();
        setupQuickActions();
        setupRecyclerViews();
        loadUserData();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        toolbar = findViewById(R.id.toolbar);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvUserName = findViewById(R.id.tv_user_name);
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        imgProfile = findViewById(R.id.img_profile);

        cardTransfer = findViewById(R.id.card_transfer);
        cardQrPay = findViewById(R.id.card_qr_pay);
        cardBillPay = findViewById(R.id.card_bill_pay);
        cardPhoneTopUp = findViewById(R.id.card_phone_topup);

        rvAccounts = findViewById(R.id.rv_accounts);
        rvTransactions = findViewById(R.id.rv_transactions);
        tvViewAllTransactions = findViewById(R.id.tv_view_all_transactions);

        cardElectricity = findViewById(R.id.card_electricity);
        cardWater = findViewById(R.id.card_water);
        cardTicket = findViewById(R.id.card_ticket);
        cardMore = findViewById(R.id.card_more);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        currentUser = sessionManager.getUser();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
    }

    private void setupNavigation() {
        // Drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Navigation view
        navigationView.setNavigationItemSelectedListener(this);

        // Update navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.nav_header_name);
        TextView navUserEmail = headerView.findViewById(R.id.nav_header_email);
        if (currentUser != null) {
            navUserName.setText(currentUser.getFullName());
            navUserEmail.setText(currentUser.getEmail());
        }

        // Bottom navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_transfer) {
                startActivity(new Intent(this, TransferActivity.class));
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, TransactionHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // Swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadUserData);
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark);
    }

    private void setupQuickActions() {
        cardTransfer.setOnClickListener(v -> {
            startActivity(new Intent(this, TransferActivity.class));
        });

        cardQrPay.setOnClickListener(v -> {
            startActivity(new Intent(this, QRScannerActivity.class));
        });

        cardBillPay.setOnClickListener(v -> {
            startActivity(new Intent(this, BillPaymentActivity.class));
        });

        cardPhoneTopUp.setOnClickListener(v -> {
            startActivity(new Intent(this, PhoneTopUpActivity.class));
        });

        // Service cards
        cardElectricity.setOnClickListener(v -> {
            Intent intent = new Intent(this, BillPaymentActivity.class);
            intent.putExtra("bill_type", "ELECTRICITY");
            startActivity(intent);
        });

        cardWater.setOnClickListener(v -> {
            Intent intent = new Intent(this, BillPaymentActivity.class);
            intent.putExtra("bill_type", "WATER");
            startActivity(intent);
        });

        cardTicket.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        cardMore.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        tvViewAllTransactions.setOnClickListener(v -> {
            startActivity(new Intent(this, TransactionHistoryActivity.class));
        });
    }

    private void setupRecyclerViews() {
        // Accounts RecyclerView
        accountList = new ArrayList<>();
        accountAdapter = new AccountAdapter(this, accountList, account -> {
            Intent intent = new Intent(this, AccountDetailActivity.class);
            intent.putExtra("account", account);
            startActivity(intent);
        });
        rvAccounts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvAccounts.setAdapter(accountAdapter);

        // Transactions RecyclerView
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList, null);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void loadUserData() {
        if (currentUser == null) return;

        // Update greeting
        tvGreeting.setText(FormatUtils.getGreeting());
        tvUserName.setText(currentUser.getFullName());

        // Load accounts (will also load transactions after accounts are loaded)
        loadAccounts();
    }

    private void loadAccounts() {
        firebaseHelper.getUserAccounts(currentUser.getId(),
            querySnapshot -> {
                swipeRefresh.setRefreshing(false);
                accountList.clear();
                double totalBalance = 0;

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null) {
                        account.setId(doc.getId());
                        accountList.add(account);
                        if (account.getAccountType() == Account.AccountType.CHECKING ||
                            account.getAccountType() == Account.AccountType.SAVING) {
                            totalBalance += account.getBalance();
                        }
                    }
                }

                accountAdapter.notifyDataSetChanged();
                tvTotalBalance.setText(FormatUtils.formatCurrency(totalBalance));

                // Load recent transactions AFTER accounts are loaded
                if (!accountList.isEmpty()) {
                    loadRecentTransactions();
                }
            },
            e -> {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(this, "Lỗi tải danh sách tài khoản", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadRecentTransactions() {
        if (accountList.isEmpty()) return;

        String accountId = accountList.get(0).getId();
        firebaseHelper.getAccountTransactions(accountId, 5,
            querySnapshot -> {
                transactionList.clear();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Transaction transaction = doc.toObject(Transaction.class);
                    if (transaction != null) {
                        transaction.setId(doc.getId());
                        transactionList.add(transaction);
                    }
                }
                transactionAdapter.notifyDataSetChanged();

                // Show/hide empty state
                if (transactionList.isEmpty()) {
                    // Show empty transactions message
                }
            },
            e -> {
                // Handle error silently for recent transactions
            }
        );
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (itemId == R.id.nav_menu_accounts) {
            // Show accounts fragment or activity
        } else if (itemId == R.id.nav_menu_transactions) {
            startActivity(new Intent(this, TransactionHistoryActivity.class));
        } else if (itemId == R.id.nav_menu_map) {
            startActivity(new Intent(this, MapActivity.class));
        } else if (itemId == R.id.nav_menu_settings) {
            // Open settings
        } else if (itemId == R.id.nav_menu_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        firebaseHelper.signOut();
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }
}
