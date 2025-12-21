package com.messkhata.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messkhata.R;
import com.messkhata.data.dao.ReportDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.MemberBalance;
import com.messkhata.ui.adapter.MemberBalanceAdapter;
import com.messkhata.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Report Fragment - View member balances and reports
 */
public class ReportFragment extends Fragment {

    // UI Components
    private TextView tvSelectedMonth;
    private TextView tvTotalExpenses;
    private TextView tvTotalMeals;
    private TextView tvMealRate;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private RecyclerView rvMemberBalances;
    
    // Adapter
    private MemberBalanceAdapter memberBalanceAdapter;
    
    // DAOs
    private ReportDao reportDao;
    
    // Session data
    private PreferenceManager prefManager;
    private int messId;
    private String userRole;
    
    // Current month
    private Calendar currentMonth;
    
    // Member balance list
    private List<MemberBalance> memberBalances = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeDAO();
        loadSessionData();
        setupListeners();
        loadReport();
    }

    private void initializeViews(View view) {
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);
        tvTotalMeals = view.findViewById(R.id.tvTotalMeals);
        tvMealRate = view.findViewById(R.id.tvMealRate);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvMemberBalances = view.findViewById(R.id.rvBalances);
        
        // Setup RecyclerView
        rvMemberBalances.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void initializeDAO() {
        reportDao = new ReportDao(requireContext());
    }

    private void loadSessionData() {
        prefManager = PreferenceManager.getInstance(requireContext());
        messId = Integer.parseInt(prefManager.getMessId());
        userRole = prefManager.getUserRole();
        currentMonth = Calendar.getInstance();
        updateMonthDisplay();
        
        // Initialize adapter
        memberBalanceAdapter = new MemberBalanceAdapter(memberBalances);
        rvMemberBalances.setAdapter(memberBalanceAdapter);
    }

    private void setupListeners() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadReport();
        });
        
        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadReport();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvSelectedMonth.setText(monthFormat.format(currentMonth.getTime()));
    }

    private void loadReport() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int year = currentMonth.get(Calendar.YEAR);
                int month = currentMonth.get(Calendar.MONTH) + 1;
                
                // Load member balances
                List<MemberBalance> balances = reportDao.getMemberBalances(messId, year, month);
                
                // Load summary data
                double totalExpenses = reportDao.getTotalExpenses(messId, year, month);
                int totalMeals = reportDao.getTotalMeals(messId, year, month);
                double mealRate = reportDao.calculateMealRate(messId, year, month);
                
                requireActivity().runOnUiThread(() -> {
                    memberBalances.clear();
                    if (balances != null) {
                        memberBalances.addAll(balances);
                    }
                    tvTotalExpenses.setText(String.format(Locale.getDefault(), "৳ %.2f", totalExpenses));
                    tvTotalMeals.setText(String.valueOf(totalMeals));
                    tvMealRate.setText(String.format(Locale.getDefault(), "৳ %.2f", mealRate));
                    
                    // Update adapter
                    memberBalanceAdapter.updateMemberBalances(memberBalances);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), 
                        "Error loading report", 
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReport();
    }
}
