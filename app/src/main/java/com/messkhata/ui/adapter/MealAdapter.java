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

import com.google.android.material.button.MaterialButton;
import com.messkhata.R;
import com.messkhata.data.model.Meal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying meal entries in a grid/list format.
 * Shows each member with their meal status (B/L/D).
 */
public class MealAdapter extends ListAdapter<MealAdapter.MemberMealData, MealAdapter.MealViewHolder> {

    private final OnMealToggleListener listener;
    private final boolean canEdit;

    public interface OnMealToggleListener {
        void onMealToggle(String userId, String userName, String mealType);
    }

    public MealAdapter(OnMealToggleListener listener, boolean canEdit) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.canEdit = canEdit;
    }

    private static final DiffUtil.ItemCallback<MemberMealData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MemberMealData>() {
                @Override
                public boolean areItemsTheSame(@NonNull MemberMealData oldItem,
                                               @NonNull MemberMealData newItem) {
                    return oldItem.userId.equals(newItem.userId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull MemberMealData oldItem,
                                                  @NonNull MemberMealData newItem) {
                    return oldItem.breakfastCount == newItem.breakfastCount &&
                           oldItem.lunchCount == newItem.lunchCount &&
                           oldItem.dinnerCount == newItem.dinnerCount;
                }
            };

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_entry, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MemberMealData data = getItem(position);
        holder.bind(data);
    }

    class MealViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMemberName;
        private final MaterialButton btnBreakfast;
        private final MaterialButton btnLunch;
        private final MaterialButton btnDinner;
        private final TextView tvTotalMeals;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            btnBreakfast = itemView.findViewById(R.id.btnBreakfast);
            btnLunch = itemView.findViewById(R.id.btnLunch);
            btnDinner = itemView.findViewById(R.id.btnDinner);
            tvTotalMeals = itemView.findViewById(R.id.tvTotalMeals);
        }

        void bind(MemberMealData data) {
            tvMemberName.setText(data.userName);

            // Update button states
            updateMealButton(btnBreakfast, data.breakfastCount);
            updateMealButton(btnLunch, data.lunchCount);
            updateMealButton(btnDinner, data.dinnerCount);

            // Calculate total
            int total = data.breakfastCount + data.lunchCount + data.dinnerCount;
            tvTotalMeals.setText(String.valueOf(total));

            // Set click listeners if can edit
            if (canEdit) {
                btnBreakfast.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMealToggle(data.userId, data.userName, Meal.BREAKFAST);
                    }
                });

                btnLunch.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMealToggle(data.userId, data.userName, Meal.LUNCH);
                    }
                });

                btnDinner.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMealToggle(data.userId, data.userName, Meal.DINNER);
                    }
                });
            } else {
                btnBreakfast.setEnabled(false);
                btnLunch.setEnabled(false);
                btnDinner.setEnabled(false);
            }
        }

        private void updateMealButton(MaterialButton button, int count) {
            if (count > 0) {
                button.setText(String.valueOf(count));
                button.setBackgroundColor(itemView.getContext()
                        .getColor(R.color.meal_active));
            } else {
                button.setText("0");
                button.setBackgroundColor(itemView.getContext()
                        .getColor(R.color.meal_inactive));
            }
        }
    }

    /**
     * Data class representing a member's meals for a day.
     */
    public static class MemberMealData {
        public final String userId;
        public final String userName;
        public int breakfastCount;
        public int lunchCount;
        public int dinnerCount;

        public MemberMealData(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            this.breakfastCount = 0;
            this.lunchCount = 0;
            this.dinnerCount = 0;
        }

        public void setMealCount(String mealType, int count) {
            switch (mealType) {
                case Meal.BREAKFAST:
                    this.breakfastCount = count;
                    break;
                case Meal.LUNCH:
                    this.lunchCount = count;
                    break;
                case Meal.DINNER:
                    this.dinnerCount = count;
                    break;
            }
        }
    }
}
