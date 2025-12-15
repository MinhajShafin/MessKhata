package com.messkhata.ui.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.messkhata.R;
import com.messkhata.data.model.Meal;
import com.messkhata.data.model.User;
import com.messkhata.ui.adapter.MealAdapter;
import com.messkhata.ui.viewmodel.AuthViewModel;
import com.messkhata.ui.viewmodel.MealViewModel;
import com.messkhata.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for daily meal tracking.
 */
public class MealFragment extends Fragment {

    private MealViewModel mealViewModel;
    private AuthViewModel authViewModel;

    private TextView tvSelectedDate;
    private TextView tvDayName;
    private ImageButton btnPrevDay;
    private ImageButton btnNextDay;
    private RecyclerView rvMeals;
    private TextView tvTotalMeals;

    private MealAdapter mealAdapter;
    private long selectedDate;
    private List<User> members = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mealViewModel = new ViewModelProvider(this).get(MealViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        selectedDate = DateUtils.getTodayStart();

        initViews(view);
        setupRecyclerView();
        setupDateNavigation();
        observeData();
    }

    private void initViews(View view) {
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvDayName = view.findViewById(R.id.tvDayName);
        btnPrevDay = view.findViewById(R.id.btnPrevDay);
        btnNextDay = view.findViewById(R.id.btnNextDay);
        rvMeals = view.findViewById(R.id.rvMeals);
        tvTotalMeals = view.findViewById(R.id.tvTotalMeals);

        updateDateDisplay();
    }

    private void setupRecyclerView() {
        boolean canEdit = mealViewModel.canEditOthersMeals();

        mealAdapter = new MealAdapter((userId, userName, mealType) -> {
            mealViewModel.toggleMeal(userId, userName, selectedDate, mealType);
        }, true); // Allow editing for now, will check permissions inside

        rvMeals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMeals.setAdapter(mealAdapter);
    }

    private void setupDateNavigation() {
        btnPrevDay.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDate);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            selectedDate = DateUtils.getStartOfDay(cal.getTimeInMillis());
            updateDateDisplay();
            loadMeals();
        });

        btnNextDay.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDate);
            cal.add(Calendar.DAY_OF_MONTH, 1);

            // Don't allow future dates
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                selectedDate = DateUtils.getStartOfDay(cal.getTimeInMillis());
                updateDateDisplay();
                loadMeals();
            }
        });

        tvSelectedDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = DateUtils.getStartOfDay(calendar.getTimeInMillis());
                    updateDateDisplay();
                    loadMeals();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void updateDateDisplay() {
        tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
        tvDayName.setText(DateUtils.getDayName(selectedDate));

        // Disable next button if today
        boolean isToday = DateUtils.isToday(selectedDate);
        btnNextDay.setEnabled(!isToday);
        btnNextDay.setAlpha(isToday ? 0.5f : 1.0f);
    }

    private void observeData() {
        // Observe members
        authViewModel.getMessMembers().observe(getViewLifecycleOwner(), users -> {
            members = users;
            loadMeals();
        });
    }

    private void loadMeals() {
        mealViewModel.getMealsByDate(selectedDate).observe(getViewLifecycleOwner(), meals -> {
            updateMealGrid(meals);
        });
    }

    private void updateMealGrid(List<Meal> meals) {
        // Create a map of meals by user ID
        Map<String, MealAdapter.MemberMealData> mealDataMap = new HashMap<>();

        // Initialize with all members
        for (User member : members) {
            mealDataMap.put(member.getId(),
                    new MealAdapter.MemberMealData(member.getId(), member.getName()));
        }

        // Fill in meal data
        int totalMeals = 0;
        for (Meal meal : meals) {
            MealAdapter.MemberMealData data = mealDataMap.get(meal.getUserId());
            if (data != null) {
                data.setMealCount(meal.getMealType(), meal.getCount());
                totalMeals += meal.getCount();
            }
        }

        // Update adapter
        mealAdapter.submitList(new ArrayList<>(mealDataMap.values()));

        // Update total
        tvTotalMeals.setText("Total Meals: " + totalMeals);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMeals();
    }
}
