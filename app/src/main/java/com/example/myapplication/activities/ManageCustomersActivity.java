package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.myapplication.R;
import com.example.myapplication.adapters.CustomerAdapter;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ManageCustomersActivity extends AppCompatActivity {

    private RecyclerView rvCustomers;
    private CustomerAdapter customerAdapter;
    private EditText etSearch;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;

    private FirebaseHelper firebaseHelper;
    private List<User> allCustomers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_customers);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadCustomers();
    }

    private void initViews() {
        rvCustomers = findViewById(R.id.rv_customers);
        etSearch = findViewById(R.id.et_search);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        fabAdd = findViewById(R.id.fab_add_customer);

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateCustomerActivity.class));
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý khách hàng");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        customerAdapter = new CustomerAdapter();
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        rvCustomers.setAdapter(customerAdapter);

        customerAdapter.setOnCustomerClickListener(new CustomerAdapter.OnCustomerClickListener() {
            @Override
            public void onCustomerClick(User user) {
                showCustomerDetails(user);
            }

            @Override
            public void onEditClick(User user) {
                editCustomer(user);
            }

            @Override
            public void onDeleteClick(User user) {
                confirmDeleteCustomer(user);
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCustomers() {
        showLoading(true);

        firebaseHelper.getFirestore()
                .collection("users")
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    allCustomers.clear();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setId(doc.getId());
                            allCustomers.add(user);
                        }
                    }

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Không thể tải danh sách khách hàng", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterCustomers(String query) {
        if (query.isEmpty()) {
            customerAdapter.setCustomers(allCustomers);
        } else {
            String lowerQuery = query.toLowerCase();
            List<User> filtered = allCustomers.stream()
                    .filter(user -> {
                        String fullName = user.getFullName() != null ? user.getFullName().toLowerCase() : "";
                        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                        String phone = user.getPhone() != null ? user.getPhone() : "";
                        String idCard = user.getIdCard() != null ? user.getIdCard() : "";
                        return fullName.contains(lowerQuery) ||
                               email.contains(lowerQuery) ||
                               phone.contains(query) ||
                               idCard.contains(query);
                    })
                    .collect(Collectors.toList());
            customerAdapter.setCustomers(filtered);
        }
        updateEmptyState();
    }

    private void updateUI() {
        customerAdapter.setCustomers(allCustomers);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (customerAdapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvCustomers.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvCustomers.setVisibility(View.VISIBLE);
        }
    }

    private void showCustomerDetails(User user) {
        // Show customer details in a dialog or new activity
        new AlertDialog.Builder(this)
                .setTitle(user.getFullName())
                .setMessage(
                        "Email: " + user.getEmail() + "\n" +
                        "Điện thoại: " + user.getPhone() + "\n" +
                        "CCCD: " + user.getIdCard() + "\n" +
                        "Trạng thái: " + (user.isVerified() ? "Đã xác minh" : "Chưa xác minh"))
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void editCustomer(User user) {
        Intent intent = new Intent(this, CreateCustomerActivity.class);
        intent.putExtra("edit_mode", true);
        intent.putExtra("customer_id", user.getId());
        startActivity(intent);
    }

    private void confirmDeleteCustomer(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa khách hàng")
                .setMessage("Bạn có chắc muốn xóa khách hàng " + user.getFullName() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteCustomer(user))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteCustomer(User user) {
        showLoading(true);
        
        firebaseHelper.getFirestore()
                .collection("users")
                .document(user.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    allCustomers.remove(user);
                    updateUI();
                    Toast.makeText(this, "Đã xóa khách hàng", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Không thể xóa khách hàng", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomers();
    }
}
