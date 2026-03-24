package com.example.myapplication.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapters.CustomerAdapter;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FirebaseHelper;
import java.util.ArrayList;
import java.util.List;

public class ManageCustomersActivity extends AppCompatActivity {
    private RecyclerView rvCustomers;
    private TextView tvEmpty;
    private List<User> customers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_customers);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvCustomers = findViewById(R.id.rvCustomers);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));

        loadCustomers();
    }

    private void loadCustomers() {
        FirebaseHelper.getInstance().getAllCustomers(
            querySnapshot -> {
                customers.clear();
                for (var doc : querySnapshot.getDocuments()) {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setId(doc.getId());
                        customers.add(user);
                    }
                }
                CustomerAdapter adapter = new CustomerAdapter(this, customers);
                rvCustomers.setAdapter(adapter);
                tvEmpty.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
            },
            e -> tvEmpty.setVisibility(View.VISIBLE)
        );
    }
}
