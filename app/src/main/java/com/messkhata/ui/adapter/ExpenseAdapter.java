package com.messkhata.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.messkhata.R;
import com.messkhata.data.model.Expense;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying expense items in RecyclerView
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private OnExpenseActionListener listener;
    private boolean isAdmin;

    public interface OnExpenseActionListener {
        void onEditExpense(Expense expense);
        void onDeleteExpense(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenseList, boolean isAdmin, OnExpenseActionListener listener) {
        this.expenseList = expenseList;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenseList != null ? expenseList.size() : 0;
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenseList = newExpenses;
        notifyDataSetChanged();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private View viewCategory;
        private TextView tvTitle;
        private TextView tvDate;
        private TextView tvAddedBy;
        private TextView tvAmount;
        private Chip chipCategory;
        private ImageButton btnEdit;
        private ImageButton btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCategory = itemView.findViewById(R.id.viewCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAddedBy = itemView.findViewById(R.id.tvAddedBy);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Expense expense) {
            // Set title, fallback to category if empty
            String title = expense.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = getCategoryDisplayName(expense.getCategory());
            }
            tvTitle.setText(title);

            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText(dateFormat.format(new Date(expense.getExpenseDate() * 1000)));

            // Set added by
            String addedByText = "Added by " + (expense.getAddedByName() != null ? expense.getAddedByName() : "Unknown");
            tvAddedBy.setText(addedByText);

            // Format amount
            tvAmount.setText(String.format(Locale.getDefault(), "৳ %.0f", expense.getAmount()));

            // Set category
            chipCategory.setText(getCategoryDisplayName(expense.getCategory()));

            // Set category color
            int categoryColor = getCategoryColor(expense.getCategory());
            viewCategory.setBackgroundColor(categoryColor);

            // Show/hide edit and delete buttons based on admin status
            if (isAdmin) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditExpense(expense);
                    }
                });

                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteExpense(expense);
                    }
                });
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }
        }

        private String getCategoryDisplayName(String category) {
            if (category == null) return "Other";
            switch (category.toLowerCase()) {
                case "grocery":
                    return "Grocery";
                case "utilities":
                    return "Utilities";
                case "cleaning":
                    return "Cleaning";
                case "gas":
                    return "Gas";
                case "rent":
                    return "Rent";
                case "miscellaneous":
                    return "Miscellaneous";
                default:
                    return category;
            }
        }

        private int getCategoryColor(String category) {
            if (category == null) return Color.parseColor("#9E9E9E");
            switch (category.toLowerCase()) {
                case "grocery":
                    return Color.parseColor("#4CAF50"); // Green
                case "utilities":
                    return Color.parseColor("#2196F3"); // Blue
                case "cleaning":
                    return Color.parseColor("#9C27B0"); // Purple
                case "gas":
                    return Color.parseColor("#FF9800"); // Orange
                case "rent":
                    return Color.parseColor("#F44336"); // Red
                case "miscellaneous":
                    return Color.parseColor("#9E9E9E"); // Grey
                default:
                    return Color.parseColor("#9E9E9E");
            }
        }
    }
}
