package com.messkhata.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.messkhata.R;
import com.messkhata.data.model.MemberBalance;
import com.messkhata.data.model.MonthlyReport;
import com.messkhata.ui.adapter.BalanceAdapter;
import com.messkhata.ui.viewmodel.ReportViewModel;
import com.messkhata.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for monthly reports and balances.
 */
public class ReportFragment extends Fragment {

    private ReportViewModel viewModel;

    private Spinner spinnerMonth;
    private TextView tvMealRate;
    private TextView tvTotalMeals;
    private TextView tvTotalMealExpenses;
    private TextView tvTotalFixedExpenses;
    private RecyclerView rvBalances;
    private MaterialButton btnGenerateReport;
    private MaterialButton btnFinalizeReport;

    private BalanceAdapter balanceAdapter;
    private MonthlyReport currentReport;

    private int selectedYear;
    private int selectedMonth;
    private List<String> monthOptions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        selectedYear = DateUtils.getCurrentYear();
        selectedMonth = DateUtils.getCurrentMonth();

        initViews(view);
        setupMonthSpinner();
        setupRecyclerView();
        setupButtons();
        loadReport();
    }

    private void initViews(View view) {
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        tvMealRate = view.findViewById(R.id.tvMealRate);
        tvTotalMeals = view.findViewById(R.id.tvTotalMeals);
        tvTotalMealExpenses = view.findViewById(R.id.tvTotalMealExpenses);
        tvTotalFixedExpenses = view.findViewById(R.id.tvTotalFixedExpenses);
        rvBalances = view.findViewById(R.id.rvBalances);
        btnGenerateReport = view.findViewById(R.id.btnGenerateReport);
        btnFinalizeReport = view.findViewById(R.id.btnFinalizeReport);
    }

    private void setupMonthSpinner() {
        // Generate last 12 months
        int year = DateUtils.getCurrentYear();
        int month = DateUtils.getCurrentMonth();

        for (int i = 0; i < 12; i++) {
            monthOptions.add(DateUtils.formatMonthYear(year, month));
            month--;
            if (month < 1) {
                month = 12;
                year--;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                monthOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(adapter);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Calculate year and month based on position
                int y = DateUtils.getCurrentYear();
                int m = DateUtils.getCurrentMonth();

                for (int i = 0; i < position; i++) {
                    m--;
                    if (m < 1) {
                        m = 12;
                        y--;
                    }
                }

                selectedYear = y;
                selectedMonth = m;
                loadReport();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        balanceAdapter = new BalanceAdapter(balance -> {
            // Show balance details
            showBalanceDetails(balance);
        });

        rvBalances.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBalances.setAdapter(balanceAdapter);
    }

    private void setupButtons() {
        btnGenerateReport.setOnClickListener(v -> {
            viewModel.generateReport(selectedYear, selectedMonth);
            loadReport();
        });

        btnFinalizeReport.setOnClickListener(v -> {
            if (currentReport != null && viewModel.isAdmin()) {
                viewModel.finalizeReport(currentReport.getId());
            }
        });

        // Hide finalize button if not admin
        btnFinalizeReport.setVisibility(viewModel.isAdmin() ? View.VISIBLE : View.GONE);
    }

    private void loadReport() {
        viewModel.getReportByMonth(selectedYear, selectedMonth)
                .observe(getViewLifecycleOwner(), report -> {
                    currentReport = report;
                    updateReportDisplay(report);

                    if (report != null) {
                        loadBalances(report.getId());
                    } else {
                        balanceAdapter.submitList(new ArrayList<>());
                    }
                });
    }

    private void updateReportDisplay(MonthlyReport report) {
        if (report != null) {
            tvMealRate.setText(String.format("৳%.2f per meal", report.getMealRate()));
            tvTotalMeals.setText(String.valueOf(report.getTotalMeals()));
            tvTotalMealExpenses.setText(String.format("৳%.2f", report.getTotalMealExpenses()));
            tvTotalFixedExpenses.setText(String.format("৳%.2f", report.getTotalFixedExpenses()));

            // Update button states
            if (report.isFinalized()) {
                btnGenerateReport.setEnabled(false);
                btnFinalizeReport.setEnabled(false);
                btnFinalizeReport.setText("Finalized");
            } else {
                btnGenerateReport.setEnabled(true);
                btnFinalizeReport.setEnabled(viewModel.isAdmin());
                btnFinalizeReport.setText("Finalize Report");
            }
        } else {
            tvMealRate.setText("N/A");
            tvTotalMeals.setText("0");
            tvTotalMealExpenses.setText("৳0.00");
            tvTotalFixedExpenses.setText("৳0.00");
            btnGenerateReport.setEnabled(true);
            btnFinalizeReport.setEnabled(false);
        }
    }

    private void loadBalances(String reportId) {
        viewModel.getBalancesByReport(reportId).observe(getViewLifecycleOwner(), balances -> {
            balanceAdapter.submitList(balances);
        });
    }

    private void showBalanceDetails(MemberBalance balance) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(balance.getUserName() + "'s Balance")
                .setMessage(String.format(
                        "Meals: %d\nMeal Cost: ৳%.2f\nFixed Share: ৳%.2f\nTotal Due: ৳%.2f\nTotal Paid: ৳%.2f\nBalance: %s৳%.2f",
                        balance.getTotalMeals(),
                        balance.getMealCost(),
                        balance.getFixedShare(),
                        balance.getTotalDue(),
                        balance.getTotalPaid(),
                        balance.getBalance() >= 0 ? "+" : "-",
                        Math.abs(balance.getBalance())
                ))
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReport();
    }
}
