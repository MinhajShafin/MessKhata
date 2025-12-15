package com.messkhata.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.messkhata.MainActivity;
import com.messkhata.R;
import com.messkhata.ui.viewmodel.AuthViewModel;

/**
 * Activity for creating or joining a mess.
 */
public class MessSetupActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    // Create Mess Views
    private View createMessLayout;
    private TextInputEditText etMessName;
    private TextInputEditText etMessAddress;
    private MaterialButton btnCreateMess;

    // Join Mess Views
    private View joinMessLayout;
    private TextInputEditText etJoinCode;
    private MaterialButton btnJoinMess;

    // Toggle Views
    private MaterialButton btnShowCreate;
    private MaterialButton btnShowJoin;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_setup);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Check if already has mess
        if (viewModel.hasJoinedMess()) {
            navigateToMain();
            return;
        }

        initViews();
        setupListeners();
        observeAuthResult();
    }

    private void initViews() {
        createMessLayout = findViewById(R.id.createMessLayout);
        etMessName = findViewById(R.id.etMessName);
        etMessAddress = findViewById(R.id.etMessAddress);
        btnCreateMess = findViewById(R.id.btnCreateMess);

        joinMessLayout = findViewById(R.id.joinMessLayout);
        etJoinCode = findViewById(R.id.etJoinCode);
        btnJoinMess = findViewById(R.id.btnJoinMess);

        btnShowCreate = findViewById(R.id.btnShowCreate);
        btnShowJoin = findViewById(R.id.btnShowJoin);
        progressBar = findViewById(R.id.progressBar);

        // Default to create view
        showCreateView();
    }

    private void setupListeners() {
        btnShowCreate.setOnClickListener(v -> showCreateView());
        btnShowJoin.setOnClickListener(v -> showJoinView());
        btnCreateMess.setOnClickListener(v -> createMess());
        btnJoinMess.setOnClickListener(v -> joinMess());
    }

    private void showCreateView() {
        createMessLayout.setVisibility(View.VISIBLE);
        joinMessLayout.setVisibility(View.GONE);
        btnShowCreate.setSelected(true);
        btnShowJoin.setSelected(false);
    }

    private void showJoinView() {
        createMessLayout.setVisibility(View.GONE);
        joinMessLayout.setVisibility(View.VISIBLE);
        btnShowCreate.setSelected(false);
        btnShowJoin.setSelected(true);
    }

    private void createMess() {
        String name = getText(etMessName);
        String address = getText(etMessAddress);

        if (name.isEmpty()) {
            etMessName.setError("Mess name is required");
            return;
        }

        showLoading(true);
        viewModel.createMess(name, address);
    }

    private void joinMess() {
        String joinCode = getText(etJoinCode).toUpperCase();

        if (joinCode.isEmpty()) {
            etJoinCode.setError("Join code is required");
            return;
        }

        if (joinCode.length() != 6) {
            etJoinCode.setError("Join code must be 6 characters");
            return;
        }

        showLoading(true);
        viewModel.joinMess(joinCode);
    }

    private void observeAuthResult() {
        viewModel.getAuthResult().observe(this, result -> {
            showLoading(false);
            if (result.success) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCreateMess.setEnabled(!show);
        btnJoinMess.setEnabled(!show);
    }
}
