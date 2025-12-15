package com.messkhata;

import android.content.Intent;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.sync.SyncWorker;
import com.messkhata.ui.activity.LoginActivity;
import com.messkhata.ui.fragment.DashboardFragment;
import com.messkhata.ui.fragment.ExpenseFragment;
import com.messkhata.ui.fragment.MealFragment;
import com.messkhata.ui.fragment.ReportFragment;
import com.messkhata.ui.fragment.SettingsFragment;
import com.messkhata.ui.viewmodel.AuthViewModel;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

/**
 * Main Activity with bottom navigation.
 */
public class MainActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private PreferenceManager prefManager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        prefManager = PreferenceManager.getInstance(this);

        // Check login status
        if (!authViewModel.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        initializeSync();
        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void initializeSync() {
        // Initialize network monitoring
        NetworkUtils networkUtils = NetworkUtils.getInstance(this);

        // Initialize Firebase sync
        MessKhataDatabase database = MessKhataDatabase.getInstance(this);
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(database);

        String messId = prefManager.getMessId();
        String userId = prefManager.getUserId();

        if (messId != null && userId != null) {
            syncManager.initSync(messId, userId);
        }

        // Schedule periodic sync
        SyncWorker.schedulePeriodicSync(this);

        // Observe network changes and trigger sync when online
        networkUtils.getNetworkAvailability().observe(this, isOnline -> {
            if (isOnline) {
                SyncWorker.triggerImmediateSync(this);
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (itemId == R.id.nav_meals) {
                fragment = new MealFragment();
            } else if (itemId == R.id.nav_expenses) {
                fragment = new ExpenseFragment();
            } else if (itemId == R.id.nav_reports) {
                fragment = new ReportFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
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
        // Stop sync
        SyncWorker.cancelSync(this);

        MessKhataDatabase database = MessKhataDatabase.getInstance(this);
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(database);
        syncManager.stopSync();

        // Sign out
        authViewModel.signOut();

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