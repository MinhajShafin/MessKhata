package com.messkhata.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.messkhata.R;
import com.messkhata.data.model.Expense;
import com.messkhata.utils.DateUtils;

/**
 * Adapter for displaying expense items.
 */
public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder> {

    private final OnExpenseClickListener listener;
    private final boolean showActions;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
        void onEditClick(Expense expense);
        void onDeleteClick(Expense expense);
    }

    public ExpenseAdapter(OnExpenseClickListener listener, boolean showActions) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.showActions = showActions;
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getAmount() == newItem.getAmount() &&
                           oldItem.getDescription().equals(newItem.getDescription()) &&
                           oldItem.getUpdatedAt() == newItem.getUpdatedAt();
                }
            };

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDescription;
        private final TextView tvAmount;
        private final TextView tvDate;
        private final TextView tvPaidBy;
        private final Chip chipCategory;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPaidBy = itemView.findViewById(R.id.tvPaidBy);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Expense expense) {
            tvDescription.setText(expense.getDescription());
            tvAmount.setText(String.format("৳%.2f", expense.getAmount()));
            tvDate.setText(DateUtils.formatDate(expense.getDate()));
            tvPaidBy.setText("Paid by: " + expense.getPaidByName());
            chipCategory.setText(expense.getCategory());

            // Set category color
            int colorRes = getCategoryColor(expense.getCategory());
            chipCategory.setChipBackgroundColorResource(colorRes);

            // Show/hide action buttons
            if (showActions) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(expense);
                    }
                });

                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(expense);
                    }
                });
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExpenseClick(expense);
                }
            });
        }

        private int getCategoryColor(String category) {
            switch (category) {
                case Expense.GROCERY:
                    return R.color.category_grocery;
                case Expense.UTILITY:
                    return R.color.category_utility;
                case Expense.GAS:
                    return R.color.category_gas;
                case Expense.RENT:
                    return R.color.category_rent;
                case Expense.MAINTENANCE:
                    return R.color.category_maintenance;
                default:
                    return R.color.category_other;
            }
        }
    }
}
