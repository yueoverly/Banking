package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.R;
import com.example.myapplication.adapters.CustomerAdapter;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for bank officer interface
 */
public class OfficerMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvGreeting, tvOfficerName, tvTotalCustomers, tvTodayTransactions;
    private CardView cardCreateCustomer, cardManageCustomers, cardManageRates, cardReports;
    private RecyclerView rvRecentCustomers;
    private FloatingActionButton fabAddCustomer;

    private CustomerAdapter customerAdapter;
    private List<User> customerList;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_main);

        initViews();
        initFirebase();
        setupToolbar();
        setupNavigation();
        setupQuickActions();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        tvGreeting = findViewById(R.id.tv_greeting);
        tvOfficerName = findViewById(R.id.tv_officer_name);
        tvTotalCustomers = findViewById(R.id.tv_total_customers);
        tvTodayTransactions = findViewById(R.id.tv_today_transactions);

        cardCreateCustomer = findViewById(R.id.card_create_customer);
        cardManageCustomers = findViewById(R.id.card_manage_customers);
        cardManageRates = findViewById(R.id.card_manage_rates);
        cardReports = findViewById(R.id.card_reports);

        rvRecentCustomers = findViewById(R.id.rv_recent_customers);
        fabAddCustomer = findViewById(R.id.fab_add_customer);
    }

    private void initFirebase() {
        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = SessionManager.getInstance(this);
        currentUser = sessionManager.getUser();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("TDTU Bank - Officer");
        }
    }

    private void setupNavigation() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Update navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.nav_header_name);
        TextView navUserEmail = headerView.findViewById(R.id.nav_header_email);
        TextView navUserRole = headerView.findViewById(R.id.nav_header_role);
        if (currentUser != null) {
            navUserName.setText(currentUser.getFullName());
            navUserEmail.setText(currentUser.getEmail());
            navUserRole.setText("Nhân viên ngân hàng");
        }

        swipeRefresh.setOnRefreshListener(this::loadData);
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark);
    }

    private void setupQuickActions() {
        cardCreateCustomer.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateCustomerActivity.class));
        });

        cardManageCustomers.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageCustomersActivity.class));
        });

        cardManageRates.setOnClickListener(v -> {
            // Open manage interest rates dialog/activity
            showManageRatesDialog();
        });

        cardReports.setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng báo cáo đang phát triển", Toast.LENGTH_SHORT).show();
        });

        fabAddCustomer.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateCustomerActivity.class));
        });
    }

    private void setupRecyclerView() {
        customerList = new ArrayList<>();
        customerAdapter = new CustomerAdapter(this, customerList, customer -> {
            // Open customer detail
            Intent intent = new Intent(this, ManageCustomersActivity.class);
            intent.putExtra("customer_id", customer.getId());
            startActivity(intent);
        });
        rvRecentCustomers.setLayoutManager(new LinearLayoutManager(this));
        rvRecentCustomers.setAdapter(customerAdapter);
        rvRecentCustomers.setNestedScrollingEnabled(false);
    }

    private void loadData() {
        if (currentUser == null) return;

        tvGreeting.setText(FormatUtils.getGreeting());
        tvOfficerName.setText(currentUser.getFullName());

        // Load customers
        loadCustomers();
    }

    private void loadCustomers() {
        firebaseHelper.getAllCustomers(
            querySnapshot -> {
                swipeRefresh.setRefreshing(false);
                customerList.clear();
                int count = 0;

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    User customer = doc.toObject(User.class);
                    if (customer != null) {
                        customer.setId(doc.getId());
                        customerList.add(customer);
                        count++;
                    }
                }

                customerAdapter.notifyDataSetChanged();
                tvTotalCustomers.setText(String.valueOf(count));
            },
            e -> {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(this, "Lỗi tải danh sách khách hàng", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void showManageRatesDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Quản lý lãi suất tiết kiệm");

        View view = getLayoutInflater().inflate(R.layout.dialog_manage_rates, null);
        final TextView tvRate3m = view.findViewById(R.id.et_rate_3m);
        final TextView tvRate6m = view.findViewById(R.id.et_rate_6m);
        final TextView tvRate12m = view.findViewById(R.id.et_rate_12m);

        // Set current rates
        tvRate3m.setText("3.5");
        tvRate6m.setText("4.5");
        tvRate12m.setText("5.5");

        builder.setView(view);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            Toast.makeText(this, "Đã cập nhật lãi suất", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_menu_dashboard) {
            // Already on dashboard
        } else if (itemId == R.id.nav_menu_customers) {
            startActivity(new Intent(this, ManageCustomersActivity.class));
        } else if (itemId == R.id.nav_menu_create_customer) {
            startActivity(new Intent(this, CreateCustomerActivity.class));
        } else if (itemId == R.id.nav_menu_rates) {
            showManageRatesDialog();
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
        loadData();
    }
}
