package com.messkhata.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.messkhata.R;
import com.messkhata.data.model.MemberBalance;

/**
 * Adapter for displaying member balances.
 */
public class BalanceAdapter extends ListAdapter<MemberBalance, BalanceAdapter.BalanceViewHolder> {

    private final OnBalanceClickListener listener;

    public interface OnBalanceClickListener {
        void onBalanceClick(MemberBalance balance);
    }

    public BalanceAdapter(OnBalanceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<MemberBalance> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MemberBalance>() {
                @Override
                public boolean areItemsTheSame(@NonNull MemberBalance oldItem,
                                               @NonNull MemberBalance newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MemberBalance oldItem,
                                                  @NonNull MemberBalance newItem) {
                    return oldItem.getBalance() == newItem.getBalance() &&
                           oldItem.getUpdatedAt() == newItem.getUpdatedAt();
                }
            };

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_balance, parent, false);
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class BalanceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMemberName;
        private final TextView tvMealCount;
        private final TextView tvMealCost;
        private final TextView tvFixedShare;
        private final TextView tvTotalDue;
        private final TextView tvTotalPaid;
        private final TextView tvBalance;
        private final View balanceIndicator;

        BalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMealCount = itemView.findViewById(R.id.tvMealCount);
            tvMealCost = itemView.findViewById(R.id.tvMealCost);
            tvFixedShare = itemView.findViewById(R.id.tvFixedShare);
            tvTotalDue = itemView.findViewById(R.id.tvTotalDue);
            tvTotalPaid = itemView.findViewById(R.id.tvTotalPaid);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            balanceIndicator = itemView.findViewById(R.id.balanceIndicator);
        }

        void bind(MemberBalance balance) {
            tvMemberName.setText(balance.getUserName());
            tvMealCount.setText(String.format("Meals: %d", balance.getTotalMeals()));
            tvMealCost.setText(String.format("Meal Cost: ৳%.2f", balance.getMealCost()));
            tvFixedShare.setText(String.format("Fixed Share: ৳%.2f", balance.getFixedShare()));
            tvTotalDue.setText(String.format("Total Due: ৳%.2f", balance.getTotalDue()));
            tvTotalPaid.setText(String.format("Total Paid: ৳%.2f", balance.getTotalPaid()));

            // Balance display
            double balanceAmount = balance.getBalance();
            if (balanceAmount > 0) {
                tvBalance.setText(String.format("+৳%.2f", balanceAmount));
                tvBalance.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.balance_positive));
                balanceIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.balance_positive));
            } else if (balanceAmount < 0) {
                tvBalance.setText(String.format("-৳%.2f", Math.abs(balanceAmount)));
                tvBalance.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.balance_negative));
                balanceIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.balance_negative));
            } else {
                tvBalance.setText("৳0.00");
                tvBalance.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.balance_neutral));
                balanceIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.balance_neutral));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBalanceClick(balance);
                }
            });
        }
    }
}
