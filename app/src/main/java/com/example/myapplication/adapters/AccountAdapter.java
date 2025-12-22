package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Account;
import com.example.myapplication.utils.FormatUtils;

import java.util.List;

public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

    private Context context;
    private List<Account> accountList;
    private OnAccountClickListener listener;

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    public AccountAdapter(Context context, List<Account> accountList, OnAccountClickListener listener) {
        this.context = context;
        this.accountList = accountList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accountList.get(position);
        holder.bind(account);
    }

    @Override
    public int getItemCount() {
        return accountList != null ? accountList.size() : 0;
    }

    class AccountViewHolder extends RecyclerView.ViewHolder {
        private CardView cardAccount;
        private ImageView ivAccountIcon;
        private TextView tvAccountType, tvAccountNumber, tvAccountBalance, tvAccountStatus;
        private TextView tvInterestRate, tvMonthlyPayment;
        private View layoutSavingInfo, layoutMortgageInfo;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAccount = itemView.findViewById(R.id.card_account);
            ivAccountIcon = itemView.findViewById(R.id.iv_account_icon);
            tvAccountType = itemView.findViewById(R.id.tv_account_type);
            tvAccountNumber = itemView.findViewById(R.id.tv_account_number);
            tvAccountBalance = itemView.findViewById(R.id.tv_account_balance);
            tvAccountStatus = itemView.findViewById(R.id.tv_account_status);
            tvInterestRate = itemView.findViewById(R.id.tv_interest_rate);
            tvMonthlyPayment = itemView.findViewById(R.id.tv_monthly_payment);
            layoutSavingInfo = itemView.findViewById(R.id.layout_saving_info);
            layoutMortgageInfo = itemView.findViewById(R.id.layout_mortgage_info);
        }

        public void bind(Account account) {
            // Set account type name and icon
            String accountTypeName;
            int iconRes;
            int cardColor;
            
            switch (account.getAccountType()) {
                case SAVING:
                    accountTypeName = "Tài khoản Tiết kiệm";
                    iconRes = R.drawable.ic_saving;
                    cardColor = context.getResources().getColor(R.color.saving_card);
                    if (layoutSavingInfo != null) {
                        layoutSavingInfo.setVisibility(View.VISIBLE);
                        tvInterestRate.setText(FormatUtils.formatInterestRate(account.getInterestRate()));
                    }
                    if (layoutMortgageInfo != null) {
                        layoutMortgageInfo.setVisibility(View.GONE);
                    }
                    break;
                case MORTGAGE:
                    accountTypeName = "Tài khoản Vay";
                    iconRes = R.drawable.ic_mortgage;
                    cardColor = context.getResources().getColor(R.color.mortgage_card);
                    if (layoutMortgageInfo != null) {
                        layoutMortgageInfo.setVisibility(View.VISIBLE);
                        tvMonthlyPayment.setText(FormatUtils.formatCurrency(account.getMonthlyPayment()));
                    }
                    if (layoutSavingInfo != null) {
                        layoutSavingInfo.setVisibility(View.GONE);
                    }
                    break;
                case CHECKING:
                default:
                    accountTypeName = "Tài khoản Thanh toán";
                    iconRes = R.drawable.ic_checking;
                    cardColor = context.getResources().getColor(R.color.checking_card);
                    if (layoutSavingInfo != null) layoutSavingInfo.setVisibility(View.GONE);
                    if (layoutMortgageInfo != null) layoutMortgageInfo.setVisibility(View.GONE);
                    break;
            }

            tvAccountType.setText(accountTypeName);
            ivAccountIcon.setImageResource(iconRes);
            cardAccount.setCardBackgroundColor(cardColor);

            // Set account number (masked)
            tvAccountNumber.setText(FormatUtils.maskAccountNumber(account.getAccountNumber()));

            // Set balance
            tvAccountBalance.setText(FormatUtils.formatCurrency(account.getBalance()));

            // Set status
            String statusText;
            int statusColor;
            switch (account.getStatus()) {
                case ACTIVE:
                    statusText = "Hoạt động";
                    statusColor = context.getResources().getColor(R.color.status_active);
                    break;
                case INACTIVE:
                    statusText = "Không hoạt động";
                    statusColor = context.getResources().getColor(R.color.status_inactive);
                    break;
                case FROZEN:
                    statusText = "Đã đóng băng";
                    statusColor = context.getResources().getColor(R.color.status_frozen);
                    break;
                case CLOSED:
                    statusText = "Đã đóng";
                    statusColor = context.getResources().getColor(R.color.status_closed);
                    break;
                default:
                    statusText = "Không xác định";
                    statusColor = context.getResources().getColor(R.color.text_secondary);
            }
            tvAccountStatus.setText(statusText);
            tvAccountStatus.setTextColor(statusColor);

            // Click listener
            cardAccount.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccountClick(account);
                }
            });
        }
    }

    public void updateData(List<Account> newList) {
        this.accountList = newList;
        notifyDataSetChanged();
    }
}
