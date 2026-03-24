package com.example.myapplication.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.SavingAccountAdapter;
import com.example.myapplication.models.Account;
import com.example.myapplication.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ManageInterestRatesActivity - Cho phép nhân viên ngân hàng sửa lãi suất
 * của tài khoản tiết kiệm theo chính sách ngân hàng
 */
public class ManageInterestRatesActivity extends AppCompatActivity 
        implements SavingAccountAdapter.OnInterestRateChangedListener {

    private Toolbar toolbar;
    private EditText etSearch;
    private RecyclerView rvSavingAccounts;
    private TextView tvNoAccounts;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private List<Account> allAccounts = new ArrayList<>();
    private List<Account> filteredAccounts = new ArrayList<>();
    private Map<String, String> customerNames = new HashMap<>();
    private SavingAccountAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_interest_rates);

        initViews();
        firebaseHelper = FirebaseHelper.getInstance();
        setupToolbar();
        setupSearch();
        loadSavingAccounts();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        rvSavingAccounts = findViewById(R.id.rvSavingAccounts);
        tvNoAccounts = findViewById(R.id.tvNoAccounts);
        progressBar = findViewById(R.id.progressBar);

        rvSavingAccounts.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                filterAccounts(s.toString());
            }
        });
    }

    private void loadSavingAccounts() {
        progressBar.setVisibility(View.VISIBLE);
        
        // First load all customers to get names
        firebaseHelper.getAllCustomers(
            customersSnapshot -> {
                for (var doc : customersSnapshot.getDocuments()) {
                    String userId = doc.getId();
                    String fullName = doc.getString("fullName");
                    if (fullName != null) {
                        customerNames.put(userId, fullName);
                    }
                }
                
                // Then load all saving accounts
                loadAccounts();
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void loadAccounts() {
        firebaseHelper.getAllSavingAccounts(
            querySnapshot -> {
                progressBar.setVisibility(View.GONE);
                allAccounts.clear();
                
                for (var doc : querySnapshot.getDocuments()) {
                    Account account = doc.toObject(Account.class);
                    if (account != null) {
                        account.setId(doc.getId());
                        allAccounts.add(account);
                    }
                }

                filteredAccounts.clear();
                filteredAccounts.addAll(allAccounts);
                
                setupAdapter();
                updateUI();
            },
            e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        );
    }

    private void setupAdapter() {
        adapter = new SavingAccountAdapter(this, filteredAccounts, customerNames, this);
        rvSavingAccounts.setAdapter(adapter);
    }

    private void filterAccounts(String query) {
        filteredAccounts.clear();
        
        if (query.isEmpty()) {
            filteredAccounts.addAll(allAccounts);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Account account : allAccounts) {
                String customerName = customerNames.get(account.getUserId());
                boolean matchName = customerName != null && customerName.toLowerCase().contains(lowerQuery);
                boolean matchNumber = account.getAccountNumber() != null && 
                                     account.getAccountNumber().contains(query);
                
                if (matchName || matchNumber) {
                    filteredAccounts.add(account);
                }
            }
        }
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateUI();
    }

    private void updateUI() {
        if (filteredAccounts.isEmpty()) {
            tvNoAccounts.setVisibility(View.VISIBLE);
            rvSavingAccounts.setVisibility(View.GONE);
        } else {
            tvNoAccounts.setVisibility(View.GONE);
            rvSavingAccounts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRateChanged() {
        // Refresh data when rate is changed
        loadSavingAccounts();
    }
}
