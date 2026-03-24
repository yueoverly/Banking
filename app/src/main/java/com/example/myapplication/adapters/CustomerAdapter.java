package com.example.myapplication.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activities.CustomerDetailActivity;
import com.example.myapplication.models.User;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {
    private final Context context;
    private final List<User> customers;

    public CustomerAdapter(Context context, List<User> customers) {
        this.context = context;
        this.customers = customers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User customer = customers.get(position);
        holder.tvName.setText(customer.getFullName());
        holder.tvEmail.setText(customer.getEmail());
        holder.tvPhone.setText(customer.getPhone());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CustomerDetailActivity.class);
            intent.putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customer.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }
    }
}
