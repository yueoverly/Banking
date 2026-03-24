package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Transaction;
import com.example.myapplication.utils.FormatUtils;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private final Context context;
    private final List<Transaction> transactions;
    private final String currentAccountId;

    public TransactionAdapter(Context context, List<Transaction> transactions, String currentAccountId) {
        this.context = context;
        this.transactions = transactions;
        this.currentAccountId = currentAccountId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        // Determine if money is coming in or going out
        boolean isIncoming = transaction.getToAccountId() != null && 
                             transaction.getToAccountId().equals(currentAccountId);
        
        // Set icon and color based on transaction type
        int iconRes;
        int colorRes;
        String typeText;
        
        switch (transaction.getType()) {
            case TRANSFER:
                iconRes = R.drawable.ic_transfer;
                typeText = isIncoming ? "Nhận tiền" : "Chuyển tiền";
                colorRes = isIncoming ? R.color.income : R.color.expense;
                break;
            case DEPOSIT:
                iconRes = R.drawable.ic_deposit;
                typeText = "Nạp tiền";
                colorRes = R.color.income;
                isIncoming = true;
                break;
            case WITHDRAWAL:
                iconRes = R.drawable.ic_withdraw;
                typeText = "Rút tiền";
                colorRes = R.color.expense;
                isIncoming = false;
                break;
            case BILL_PAYMENT:
                iconRes = R.drawable.ic_bill;
                typeText = "Thanh toán hóa đơn";
                colorRes = R.color.expense;
                isIncoming = false;
                break;
            case PHONE_TOPUP:
                iconRes = R.drawable.ic_topup;
                typeText = "Nạp điện thoại";
                colorRes = R.color.expense;
                isIncoming = false;
                break;
            case LOAN_PAYMENT:
                iconRes = R.drawable.ic_mortgage;
                typeText = "Trả nợ vay";
                colorRes = R.color.expense;
                isIncoming = false;
                break;
            default:
                iconRes = R.drawable.ic_transfer;
                typeText = "Giao dịch";
                colorRes = R.color.text_secondary;
        }
        
        holder.ivIcon.setImageResource(iconRes);
        holder.tvType.setText(typeText);
        
        // Description
        String description = transaction.getDescription();
        if (description == null || description.isEmpty()) {
            description = transaction.getRecipientName() != null ? transaction.getRecipientName() : typeText;
        }
        holder.tvDescription.setText(description);
        
        // Amount with sign
        String amountText = (isIncoming ? "+" : "-") + FormatUtils.formatCurrency(transaction.getAmount());
        holder.tvAmount.setText(amountText);
        holder.tvAmount.setTextColor(ContextCompat.getColor(context, colorRes));
        
        // Date
        holder.tvDate.setText(FormatUtils.formatDateTime(transaction.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvType, tvDescription, tvAmount, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvType = itemView.findViewById(R.id.tvType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
