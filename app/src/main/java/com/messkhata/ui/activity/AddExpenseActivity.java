package com.messkhata.ui.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.R;
import com.messkhata.data.model.Expense;
import com.messkhata.ui.viewmodel.ExpenseViewModel;
import com.messkhata.utils.DateUtils;

import java.util.Calendar;

/**
 * Activity for adding or editing expenses.
 */
public class AddExpenseActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;

    private AutoCompleteTextView spinnerCategory;
    private TextInputEditText etDescription;
    private TextInputEditText etAmount;
    private TextInputEditText etDate;
    private SwitchMaterial switchSharedEqually;
    private SwitchMaterial switchIncludeInMealRate;
    private MaterialButton btnSave;

    private long selectedDate;
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        initViews();
        setupCategoryDropdown();
        setupDatePicker();
        setupListeners();
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        switchSharedEqually = findViewById(R.id.switchSharedEqually);
        switchIncludeInMealRate = findViewById(R.id.switchIncludeInMealRate);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // Set default date to today
        selectedDate = DateUtils.getTodayStart();
        etDate.setText(DateUtils.formatDate(selectedDate));
    }

    private void setupCategoryDropdown() {
        String[] categories = viewModel.getCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];
            updateMealRateSwitch();
        });

        // Default category
        selectedCategory = Expense.GROCERY;
        spinnerCategory.setText(selectedCategory, false);
        switchIncludeInMealRate.setChecked(true);
    }

    private void updateMealRateSwitch() {
        // Grocery and Gas are included in meal rate by default
        boolean includeInMealRate = Expense.GROCERY.equals(selectedCategory) ||
                                    Expense.GAS.equals(selectedCategory);
        switchIncludeInMealRate.setChecked(includeInMealRate);
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = DateUtils.getStartOfDay(calendar.getTimeInMillis());
                    etDate.setText(DateUtils.formatDate(selectedDate));
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

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            return;
        }

        if (amountStr.isEmpty()) {
            etAmount.setError("Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return;
        }

        boolean isSharedEqually = switchSharedEqually.isChecked();
        boolean isIncludedInMealRate = switchIncludeInMealRate.isChecked();

        viewModel.addExpense(selectedCategory, description, amount, selectedDate,
                            isSharedEqually, isIncludedInMealRate);

        Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
