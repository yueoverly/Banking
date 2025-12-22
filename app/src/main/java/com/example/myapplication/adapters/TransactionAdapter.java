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

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private Context context;
    private List<Transaction> transactionList;
    private String currentAccountId;
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(Context context, List<Transaction> transactionList, String currentAccountId) {
        this.context = context;
        this.transactionList = transactionList;
        this.currentAccountId = currentAccountId;
    }

    public TransactionAdapter(Context context, List<Transaction> transactionList, String currentAccountId, OnTransactionClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.currentAccountId = currentAccountId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivTransactionIcon;
        private TextView tvTransactionType, tvTransactionDescription, tvTransactionTime;
        private TextView tvTransactionAmount, tvTransactionStatus;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTransactionIcon = itemView.findViewById(R.id.iv_transaction_icon);
            tvTransactionType = itemView.findViewById(R.id.tv_transaction_type);
            tvTransactionDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvTransactionTime = itemView.findViewById(R.id.tv_transaction_time);
            tvTransactionAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvTransactionStatus = itemView.findViewById(R.id.tv_transaction_status);
        }

        public void bind(Transaction transaction) {
            // Determine if incoming or outgoing
            boolean isIncoming = transaction.getToAccountId() != null && 
                    transaction.getToAccountId().equals(currentAccountId);
            boolean isDeposit = transaction.getType() == Transaction.TransactionType.DEPOSIT;
            boolean isWithdrawal = transaction.getType() == Transaction.TransactionType.WITHDRAWAL;

            // Set icon and colors based on transaction type
            int iconRes;
            int amountColor;
            String amountPrefix;
            String typeText;
            String description;

            switch (transaction.getType()) {
                case TRANSFER_INTERNAL:
                    iconRes = isIncoming ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up;
                    typeText = isIncoming ? "Nhận tiền" : "Chuyển tiền";
                    description = isIncoming ? "Từ: " + FormatUtils.maskAccountNumber(transaction.getFromAccountId()) 
                            : "Đến: " + FormatUtils.maskAccountNumber(transaction.getToAccountId());
                    amountColor = isIncoming ? R.color.income_green : R.color.expense_red;
                    amountPrefix = isIncoming ? "+" : "-";
                    break;
                    
                case TRANSFER_EXTERNAL:
                    iconRes = isIncoming ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up;
                    typeText = isIncoming ? "Nhận tiền liên ngân hàng" : "Chuyển tiền liên ngân hàng";
                    description = isIncoming ? "Từ ngân hàng khác" : "Đến ngân hàng khác";
                    amountColor = isIncoming ? R.color.income_green : R.color.expense_red;
                    amountPrefix = isIncoming ? "+" : "-";
                    break;
                    
                case DEPOSIT:
                    iconRes = R.drawable.ic_deposit;
                    typeText = "Nạp tiền";
                    description = "Nạp tiền vào tài khoản";
                    amountColor = R.color.income_green;
                    amountPrefix = "+";
                    break;
                    
                case WITHDRAWAL:
                    iconRes = R.drawable.ic_withdraw;
                    typeText = "Rút tiền";
                    description = "Rút tiền từ tài khoản";
                    amountColor = R.color.expense_red;
                    amountPrefix = "-";
                    break;
                    
                case BILL_PAYMENT:
                    iconRes = R.drawable.ic_bill;
                    typeText = "Thanh toán hóa đơn";
                    description = transaction.getDescription() != null ? transaction.getDescription() : "Thanh toán hóa đơn";
                    amountColor = R.color.expense_red;
                    amountPrefix = "-";
                    break;
                    
                case PHONE_TOPUP:
                    iconRes = R.drawable.ic_phone;
                    typeText = "Nạp tiền điện thoại";
                    description = transaction.getDescription() != null ? transaction.getDescription() : "Nạp tiền điện thoại";
                    amountColor = R.color.expense_red;
                    amountPrefix = "-";
                    break;
                    
                case QR_PAYMENT:
                    iconRes = R.drawable.ic_qr_code;
                    typeText = "Thanh toán QR";
                    description = transaction.getDescription() != null ? transaction.getDescription() : "Thanh toán qua mã QR";
                    amountColor = R.color.expense_red;
                    amountPrefix = "-";
                    break;
                    
                default:
                    iconRes = R.drawable.ic_transaction;
                    typeText = "Giao dịch";
                    description = transaction.getDescription() != null ? transaction.getDescription() : "Giao dịch khác";
                    amountColor = R.color.text_secondary;
                    amountPrefix = "";
            }

            ivTransactionIcon.setImageResource(iconRes);
            tvTransactionType.setText(typeText);
            tvTransactionDescription.setText(description);

            // Format amount
            String amountText = amountPrefix + FormatUtils.formatCurrency(transaction.getAmount());
            tvTransactionAmount.setText(amountText);
            tvTransactionAmount.setTextColor(ContextCompat.getColor(context, amountColor));

            // Format time
            tvTransactionTime.setText(FormatUtils.formatRelativeTime(transaction.getCreatedAt()));

            // Set status
            String statusText;
            int statusColor;
            switch (transaction.getStatus()) {
                case COMPLETED:
                    statusText = "Thành công";
                    statusColor = R.color.status_active;
                    break;
                case PENDING:
                    statusText = "Đang xử lý";
                    statusColor = R.color.status_pending;
                    break;
                case FAILED:
                    statusText = "Thất bại";
                    statusColor = R.color.status_closed;
                    break;
                case CANCELLED:
                    statusText = "Đã hủy";
                    statusColor = R.color.status_inactive;
                    break;
                default:
                    statusText = "";
                    statusColor = R.color.text_secondary;
            }
            tvTransactionStatus.setText(statusText);
            tvTransactionStatus.setTextColor(ContextCompat.getColor(context, statusColor));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });
        }
    }

    public void updateData(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    public void setCurrentAccountId(String accountId) {
        this.currentAccountId = accountId;
        notifyDataSetChanged();
    }
}
