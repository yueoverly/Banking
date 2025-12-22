package com.example.myapplication.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.example.myapplication.R;
import com.example.myapplication.adapters.TransactionAdapter;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private String accountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = new SessionManager(this);

        accountId = getIntent().getStringExtra("account_id");

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadTransactions();
    }

    private void initViews() {
        rvTransactions = findViewById(R.id.rv_transactions);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử giao dịch");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);

        transactionAdapter.setOnTransactionClickListener(transaction -> {
            // Show transaction details dialog
            showTransactionDetails(transaction);
        });
    }

    private void loadTransactions() {
        showLoading(true);

        String userId = sessionManager.getUserId();
        Query query;

        if (accountId != null && !accountId.isEmpty()) {
            // Load transactions for specific account
            query = firebaseHelper.getFirestore()
                    .collection("transactions")
                    .whereEqualTo("accountId", accountId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            // Load all transactions for user
            query = firebaseHelper.getFirestore()
                    .collection("transactions")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    List<Transaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction transaction = doc.toObject(Transaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    
                    if (transactions.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvTransactions.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.VISIBLE);
                        transactionAdapter.setTransactions(transactions);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Không thể tải lịch sử giao dịch", Toast.LENGTH_SHORT).show();
                });
    }

    private void showTransactionDetails(Transaction transaction) {
        // Could show a dialog with full transaction details
        Toast.makeText(this, "Mã GD: " + transaction.getId(), Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
