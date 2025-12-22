package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.models.User;
import com.example.myapplication.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {

    private Context context;
    private List<User> customerList = new ArrayList<>();
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(User customer);
        void onEditClick(User customer);
        void onDeleteClick(User customer);
    }

    // Default constructor
    public CustomerAdapter() {
        this.customerList = new ArrayList<>();
    }

    public CustomerAdapter(Context context, List<User> customerList, OnCustomerClickListener listener) {
        this.context = context;
        this.customerList = customerList != null ? customerList : new ArrayList<>();
        this.listener = listener;
    }
    
    public CustomerAdapter(List<User> customerList, OnCustomerClickListener listener) {
        this.customerList = customerList != null ? customerList : new ArrayList<>();
        this.listener = listener;
    }
    
    public void setOnCustomerClickListener(OnCustomerClickListener listener) {
        this.listener = listener;
    }
    
    public void setCustomers(List<User> customers) {
        this.customerList = customers != null ? customers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        User customer = customerList.get(position);
        holder.bind(customer);
    }

    @Override
    public int getItemCount() {
        return customerList != null ? customerList.size() : 0;
    }

    class CustomerViewHolder extends RecyclerView.ViewHolder {
        private CardView cardCustomer;
        private ImageView ivCustomerAvatar;
        private TextView tvCustomerName, tvCustomerEmail, tvCustomerPhone;
        private TextView tvCustomerIdCard, tvCustomerStatus;
        private ImageView ivVerified;
        private ImageButton btnEdit, btnDelete;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCustomer = itemView.findViewById(R.id.card_customer);
            ivCustomerAvatar = itemView.findViewById(R.id.iv_customer_avatar);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvCustomerEmail = itemView.findViewById(R.id.tv_customer_email);
            tvCustomerPhone = itemView.findViewById(R.id.tv_customer_phone);
            tvCustomerIdCard = itemView.findViewById(R.id.tv_customer_id_card);
            tvCustomerStatus = itemView.findViewById(R.id.tv_customer_status);
            ivVerified = itemView.findViewById(R.id.iv_verified);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(User customer) {
            // Set customer name
            tvCustomerName.setText(customer.getFullName());

            // Set email
            tvCustomerEmail.setText(customer.getEmail());

            // Set phone
            if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
                tvCustomerPhone.setText(FormatUtils.formatPhoneNumber(customer.getPhone()));
                tvCustomerPhone.setVisibility(View.VISIBLE);
            } else {
                tvCustomerPhone.setVisibility(View.GONE);
            }

            // Set ID card (masked)
            String idCard = customer.getIdCard() != null ? customer.getIdCard() : customer.getIdCardNumber();
            if (idCard != null && !idCard.isEmpty()) {
                String maskedId = idCard.length() > 4 ? 
                        "***" + idCard.substring(idCard.length() - 4) :
                        idCard;
                tvCustomerIdCard.setText("CCCD: " + maskedId);
                tvCustomerIdCard.setVisibility(View.VISIBLE);
            } else {
                tvCustomerIdCard.setVisibility(View.GONE);
            }

            // Set verification status
            if (customer.isVerified()) {
                tvCustomerStatus.setText("Đã xác minh");
                tvCustomerStatus.setTextColor(ContextCompat.getColor(context, R.color.status_active));
                if (ivVerified != null) ivVerified.setVisibility(View.VISIBLE);
            } else {
                tvCustomerStatus.setText("Chưa xác minh");
                tvCustomerStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending));
                if (ivVerified != null) ivVerified.setVisibility(View.GONE);
            }

            // Load profile image
            if (customer.getProfileImageUrl() != null && !customer.getProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(customer.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivCustomerAvatar);
            } else {
                ivCustomerAvatar.setImageResource(R.drawable.ic_person);
            }

            // Click listeners
            if (cardCustomer != null) {
                cardCustomer.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCustomerClick(customer);
                    }
                });
            }
            
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(customer);
                    }
                });
            }
            
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(customer);
                    }
                });
            }
        }
    }

    public void updateData(List<User> newList) {
        this.customerList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void filterByName(String query) {
        // Implement search filter if needed
    }
}
