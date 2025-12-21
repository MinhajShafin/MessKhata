package com.messkhata.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.messkhata.R;
import com.messkhata.data.model.Meal;

import java.util.List;

/**
 * Adapter for displaying meal entry items in RecyclerView
 * Used for calendar view showing meals for each member on a specific date
 */
public class MealEntryAdapter extends RecyclerView.Adapter<MealEntryAdapter.MealEntryViewHolder> {

    private List<Meal> mealList;
    private OnMealChangeListener listener;
    private boolean isEditable;

    public interface OnMealChangeListener {
        void onMealCountChanged(Meal meal, String mealType, int newCount);
    }

    public MealEntryAdapter(List<Meal> mealList, boolean isEditable, OnMealChangeListener listener) {
        this.mealList = mealList;
        this.isEditable = isEditable;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_entry, parent, false);
        return new MealEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealEntryViewHolder holder, int position) {
        Meal meal = mealList.get(position);
        holder.bind(meal);
    }

    @Override
    public int getItemCount() {
        return mealList != null ? mealList.size() : 0;
    }

    public void updateMeals(List<Meal> newMeals) {
        this.mealList = newMeals;
        notifyDataSetChanged();
    }

    class MealEntryViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMemberName;
        private MaterialButton btnBreakfast;
        private MaterialButton btnLunch;
        private MaterialButton btnDinner;

        public MealEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            btnBreakfast = itemView.findViewById(R.id.btnBreakfast);
            btnLunch = itemView.findViewById(R.id.btnLunch);
            btnDinner = itemView.findViewById(R.id.btnDinner);
        }

        public void bind(Meal meal) {
            // Note: Member name would need to be added to Meal model or fetched separately
            // For now, showing user ID
            tvMemberName.setText("Member #" + meal.getUserId());

            // Set meal counts
            btnBreakfast.setText(String.valueOf(meal.getBreakfast()));
            btnLunch.setText(String.valueOf(meal.getLunch()));
            btnDinner.setText(String.valueOf(meal.getDinner()));

            if (isEditable) {
                // Enable click listeners for editing
                btnBreakfast.setEnabled(true);
                btnLunch.setEnabled(true);
                btnDinner.setEnabled(true);

                btnBreakfast.setOnClickListener(v -> {
                    int newCount = (meal.getBreakfast() + 1) % 6; // Cycle 0-5
                    meal.setBreakfast(newCount);
                    btnBreakfast.setText(String.valueOf(newCount));
                    if (listener != null) {
                        listener.onMealCountChanged(meal, "breakfast", newCount);
                    }
                });

                btnLunch.setOnClickListener(v -> {
                    int newCount = (meal.getLunch() + 1) % 6; // Cycle 0-5
                    meal.setLunch(newCount);
                    btnLunch.setText(String.valueOf(newCount));
                    if (listener != null) {
                        listener.onMealCountChanged(meal, "lunch", newCount);
                    }
                });

                btnDinner.setOnClickListener(v -> {
                    int newCount = (meal.getDinner() + 1) % 6; // Cycle 0-5
                    meal.setDinner(newCount);
                    btnDinner.setText(String.valueOf(newCount));
                    if (listener != null) {
                        listener.onMealCountChanged(meal, "dinner", newCount);
                    }
                });
            } else {
                // Disable editing
                btnBreakfast.setEnabled(false);
                btnLunch.setEnabled(false);
                btnDinner.setEnabled(false);
            }
        }
    }
}
