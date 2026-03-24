package com.example.myapplication.adapters;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.utils.FirebaseHelper;
import com.example.myapplication.utils.FormatUtils;

import java.util.List;
import java.util.Map;

public class SavingAccountAdapter extends RecyclerView.Adapter<SavingAccountAdapter.ViewHolder> {

    private final Context context;
    private final List<Account> accounts;
    private final Map<String, String> customerNames; // accountId -> customerName
    private final OnInterestRateChangedListener listener;

    public interface OnInterestRateChangedListener {
        void onRateChanged();
    }

    public SavingAccountAdapter(Context context, List<Account> accounts, 
                                Map<String, String> customerNames,
                                OnInterestRateChangedListener listener) {
        this.context = context;
        this.accounts = accounts;
        this.customerNames = customerNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saving_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Account account = accounts.get(position);
        
        String customerName = customerNames.get(account.getUserId());
        holder.tvCustomerName.setText(customerName != null ? customerName : "Khách hàng");
        holder.tvAccountNumber.setText(account.getFormattedAccountNumber());
        holder.tvBalance.setText(FormatUtils.formatCurrency(account.getBalance()));
        holder.tvInterestRate.setText(FormatUtils.formatInterestRate(account.getInterestRate()));

        holder.btnEditRate.setOnClickListener(v -> showEditRateDialog(account));
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    private void showEditRateDialog(Account account) {
        EditText input = new EditText(context);
        input.setHint("Nhập lãi suất mới (%)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(account.getInterestRate()));
        input.setPadding(48, 32, 48, 32);

        String customerName = customerNames.get(account.getUserId());

        new AlertDialog.Builder(context)
                .setTitle("Chỉnh sửa lãi suất")
                .setMessage("Khách hàng: " + (customerName != null ? customerName : "N/A") +
                           "\nSố TK: " + account.getFormattedAccountNumber() +
                           "\nSố dư: " + FormatUtils.formatCurrency(account.getBalance()) +
                           "\n\nLãi suất hiện tại: " + FormatUtils.formatInterestRate(account.getInterestRate()))
                .setView(input)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    try {
                        double newRate = Double.parseDouble(input.getText().toString());
                        if (newRate < 0 || newRate > 20) {
                            Toast.makeText(context, "Lãi suất phải từ 0% đến 20%", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updateInterestRate(account, newRate);
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateInterestRate(Account account, double newRate) {
        FirebaseHelper.getInstance().updateAccountInterestRate(account.getId(), newRate,
            aVoid -> {
                Toast.makeText(context, "✅ Cập nhật lãi suất thành công!", Toast.LENGTH_SHORT).show();
                account.setInterestRate(newRate);
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onRateChanged();
                }
            },
            e -> Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvAccountNumber, tvBalance, tvInterestRate;
        Button btnEditRate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvInterestRate = itemView.findViewById(R.id.tvInterestRate);
            btnEditRate = itemView.findViewById(R.id.btnEditRate);
        }
    }
}
