package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.BankBranch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

    private List<BankBranch> branches = new ArrayList<>();
    private OnBranchClickListener listener;
    private int selectedPosition = -1;

    public interface OnBranchClickListener {
        void onBranchClick(BankBranch branch, int position);
        void onDirectionClick(BankBranch branch);
    }

    public void setOnBranchClickListener(OnBranchClickListener listener) {
        this.listener = listener;
    }

    public void setBranches(List<BankBranch> branches) {
        this.branches = branches != null ? branches : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_branch, parent, false);
        return new BranchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        BankBranch branch = branches.get(position);
        holder.bind(branch, position);
    }

    @Override
    public int getItemCount() {
        return branches.size();
    }

    class BranchViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBranchName;
        private final TextView tvBranchAddress;
        private final TextView tvDistance;
        private final TextView tvWorkingHours;
        private final ImageButton btnDirection;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBranchName = itemView.findViewById(R.id.tv_branch_name);
            tvBranchAddress = itemView.findViewById(R.id.tv_branch_address);
            tvDistance = itemView.findViewById(R.id.tv_branch_distance);
            tvWorkingHours = itemView.findViewById(R.id.tv_branch_hours);
            btnDirection = itemView.findViewById(R.id.btn_navigate);
        }

        public void bind(BankBranch branch, int position) {
            tvBranchName.setText(branch.getName());
            tvBranchAddress.setText(branch.getAddress());
            tvWorkingHours.setText(branch.getWorkingHours());

            // Format distance - use getFormattedDistance() or getDistanceFromUser()
            tvDistance.setText(branch.getFormattedDistance());

            // Highlight selected item
            itemView.setSelected(position == selectedPosition);

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBranchClick(branch, position);
                }
            });

            btnDirection.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDirectionClick(branch);
                }
            });
        }
    }
}
