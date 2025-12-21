package com.messkhata.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.R;
import com.messkhata.data.dao.ExpenseDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.utils.DateUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.Calendar;

/**
 * Activity for adding expenses.
 */
public class AddExpenseActivity extends AppCompatActivity {

    // UI Components
    private TextInputEditText etDescription;
    private TextInputEditText etAmount;
    private TextView tvSelectedDate;
    private MaterialCardView cardDate;
    private ChipGroup chipGroupCategory;
    private MaterialButton btnSave;
    private ProgressBar progressBar;
    
    // DAOs
    private ExpenseDao expenseDao;
    
    // Session data
    private PreferenceManager prefManager;
    private int messId;
    private int userId;

    // Selected values
    private long selectedDate;
    private String selectedCategory = "Grocery"; // Default category

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        initViews();
        initDAO();
        loadSessionData();
        setupDatePicker();
        setupCategoryChips();
        setupListeners();
    }

    private void initViews() {
        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        cardDate = findViewById(R.id.cardDate);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Set default date to today
        selectedDate = DateUtils.getTodayStart();
        tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
    }

    private void initDAO() {
        expenseDao = new ExpenseDao(this);
    }

    private void loadSessionData() {
        prefManager = new PreferenceManager(this);
        messId = Integer.parseInt(prefManager.getMessId());
        userId = Integer.parseInt(prefManager.getUserId());
    }

    private void setupDatePicker() {
        cardDate.setOnClickListener(v -> showDatePicker());
    }

    private void setupCategoryChips() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    selectedCategory = chip.getText().toString();
                }
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = DateUtils.getStartOfDay(calendar.getTimeInMillis());
                    tvSelectedDate.setText(DateUtils.formatDate(selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Don't allow future dates
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveExpense());
    }

    private void saveExpense() {
        String description = getText(etDescription);
        String amountStr = getText(etAmount);

        // Validation
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be positive");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            etAmount.requestFocus();
            return;
        }

        // Validate date is not in future
        if (selectedDate > System.currentTimeMillis() / 1000) {
            Toast.makeText(this, "Date cannot be in the future", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        showLoading(true);

        // Save expense in background thread
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long expenseId = expenseDao.addExpense(
                    messId,
                    userId,
                    selectedCategory,
                    amount,
                    description,
                    selectedDate
                );

                runOnUiThread(() -> {
                    showLoading(false);
                    if (expenseId > 0) {
                        Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to add expense", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        etDescription.setEnabled(!show);
        etAmount.setEnabled(!show);
        cardDate.setEnabled(!show);
        chipGroupCategory.setEnabled(!show);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
