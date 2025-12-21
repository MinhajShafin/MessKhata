package com.messkhata.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.messkhata.R;
import com.messkhata.data.model.MemberBalance;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying member balance items in RecyclerView
 */
public class MemberBalanceAdapter extends RecyclerView.Adapter<MemberBalanceAdapter.MemberBalanceViewHolder> {

    private List<MemberBalance> memberBalanceList;

    public MemberBalanceAdapter(List<MemberBalance> memberBalanceList) {
        this.memberBalanceList = memberBalanceList;
    }

    @NonNull
    @Override
    public MemberBalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_balance, parent, false);
        return new MemberBalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberBalanceViewHolder holder, int position) {
        MemberBalance memberBalance = memberBalanceList.get(position);
        holder.bind(memberBalance);
    }

    @Override
    public int getItemCount() {
        return memberBalanceList != null ? memberBalanceList.size() : 0;
    }

    public void updateMemberBalances(List<MemberBalance> newMemberBalances) {
        this.memberBalanceList = newMemberBalances;
        notifyDataSetChanged();
    }

    class MemberBalanceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMemberName;
        private TextView tvMeals;
        private TextView tvBalance;
        private TextView tvStatus;

        public MemberBalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMeals = itemView.findViewById(R.id.tvMeals);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(MemberBalance memberBalance) {
            // Set member name
            tvMemberName.setText(memberBalance.getFullName());

            // Set meals info
            String mealsText = memberBalance.getTotalMeals() + " meals";
            tvMeals.setText(mealsText);

            // Set balance (due amount)
            double dueAmount = memberBalance.getDueAmount();
            String balanceText;
            if (dueAmount > 0) {
                balanceText = String.format(Locale.getDefault(), "৳ %.0f", dueAmount);
                tvBalance.setTextColor(Color.parseColor("#F44336")); // Red for due
            } else if (dueAmount < 0) {
                balanceText = String.format(Locale.getDefault(), "৳ %.0f", Math.abs(dueAmount));
                tvBalance.setTextColor(Color.parseColor("#4CAF50")); // Green for overpaid
            } else {
                balanceText = "৳ 0";
                tvBalance.setTextColor(Color.parseColor("#4CAF50")); // Green for paid
            }
            tvBalance.setText(balanceText);

            // Set payment status
            String status = memberBalance.getPaymentStatus();
            tvStatus.setText(status.toUpperCase());

            // Color-code status
            int statusColor;
            int statusBgColor;
            switch (status.toLowerCase()) {
                case "paid":
                    statusColor = Color.parseColor("#4CAF50"); // Green
                    statusBgColor = Color.parseColor("#E8F5E9"); // Light green background
                    break;
                case "partial":
                    statusColor = Color.parseColor("#FF9800"); // Orange
                    statusBgColor = Color.parseColor("#FFF3E0"); // Light orange background
                    break;
                case "pending":
                default:
                    statusColor = Color.parseColor("#F44336"); // Red
                    statusBgColor = Color.parseColor("#FFEBEE"); // Light red background
                    break;
            }
            tvStatus.setTextColor(statusColor);
            tvStatus.setBackgroundColor(statusBgColor);
        }
    }
}
