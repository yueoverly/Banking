package com.example.myapplication.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activities.AccountDetailActivity;
import com.example.myapplication.models.Account;
import com.example.myapplication.utils.FormatUtils;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    private final Context context;
    private final List<Account> accounts;

    public AccountAdapter(Context context, List<Account> accounts) {
        this.context = context;
        this.accounts = accounts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account account = accounts.get(position);
        
        // Set account type name and icon
        switch (account.getAccountType()) {
            case CHECKING:
                holder.tvAccountType.setText("Tài khoản Thanh toán");
                holder.ivIcon.setImageResource(R.drawable.ic_checking);
                holder.cardBackground.setBackgroundResource(R.drawable.bg_gradient_primary);
                break;
            case SAVING:
                holder.tvAccountType.setText("Tài khoản Tiết kiệm");
                holder.ivIcon.setImageResource(R.drawable.ic_saving);
                holder.cardBackground.setBackgroundResource(R.drawable.bg_gradient_saving);
                break;
            case MORTGAGE:
                holder.tvAccountType.setText("Tài khoản Vay");
                holder.ivIcon.setImageResource(R.drawable.ic_mortgage);
                holder.cardBackground.setBackgroundResource(R.drawable.bg_gradient_mortgage);
                break;
        }
        
        holder.tvAccountNumber.setText(account.getFormattedAccountNumber());
        holder.tvBalance.setText(FormatUtils.formatCurrency(account.getBalance()));
        
        // Status
        String statusText = "Hoạt động";
        if (account.getStatus() != null) {
            switch (account.getStatus()) {
                case ACTIVE: statusText = "Hoạt động"; break;
                case INACTIVE: statusText = "Không hoạt động"; break;
                case FROZEN: statusText = "Đóng băng"; break;
                case CLOSED: statusText = "Đã đóng"; break;
            }
        }
        holder.tvStatus.setText(statusText);
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AccountDetailActivity.class);
            intent.putExtra("account_id", account.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout cardBackground;
        ImageView ivIcon;
        TextView tvAccountType, tvAccountNumber, tvBalance, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBackground = itemView.findViewById(R.id.cardBackground);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvAccountType = itemView.findViewById(R.id.tvAccountType);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
