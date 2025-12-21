package com.messkhata;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.ui.activity.LoginActivity;
import com.messkhata.ui.activity.MessSetupActivity;
import com.messkhata.ui.fragment.DashboardFragment;
import com.messkhata.ui.fragment.ExpenseFragment;
import com.messkhata.ui.fragment.MealFragment;
import com.messkhata.ui.fragment.ReportFragment;
import com.messkhata.ui.fragment.SettingsFragment;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

/**
 * Main Activity with bottom navigation.
 */
public class MainActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    private BottomNavigationView bottomNav;
    
    // Fragment instances (reused to maintain state)
    private DashboardFragment dashboardFragment;
    private MealFragment mealFragment;
    private ExpenseFragment expenseFragment;
    private ReportFragment reportFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is logged in
        SharedPreferences prefs = getSharedPreferences("MessKhataPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {
            // Not logged in - redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Check if user has mess
        int messId = prefs.getInt("messId", -1);
        if (messId == -1) {
            // User logged in but no mess - redirect to setup
            Intent intent = new Intent(this, MessSetupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // User is authenticated and has mess - continue
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize PreferenceManager
        prefManager = new PreferenceManager(this);

        initializeSync();
        setupBottomNavigation();

    }

    private void initializeSync() {
        // Initialize network monitoring
        NetworkUtils networkUtils = NetworkUtils.getInstance(this);

        // Initialize Firebase sync
        MessKhataDatabase database = MessKhataDatabase.getInstance(this);

        String messId = prefManager.getMessId();
        String userId = prefManager.getUserId();
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        
        // Initialize fragments (lazy initialization - created once and reused)
        dashboardFragment = new DashboardFragment();
        mealFragment = new MealFragment();
        expenseFragment = new ExpenseFragment();
        reportFragment = new ReportFragment();
        settingsFragment = new SettingsFragment();
        
        // Load default fragment (Dashboard)
        loadFragment(dashboardFragment);
        
        // Setup navigation item selected listener
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                selectedFragment = dashboardFragment;
            } else if (itemId == R.id.nav_meals) {
                selectedFragment = mealFragment;
            } else if (itemId == R.id.nav_expenses) {
                selectedFragment = expenseFragment;
            } else if (itemId == R.id.nav_reports) {
                selectedFragment = reportFragment;
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = settingsFragment;
            }
            
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Clear session data
        SharedPreferences prefs = getSharedPreferences("MessKhataPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        MessKhataDatabase database = MessKhataDatabase.getInstance(this);
        // Navigate to login
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup if needed
    }
}