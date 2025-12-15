package com.messkhata.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.messkhata.R;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.User;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.sync.SyncWorker;
import com.messkhata.ui.activity.LoginActivity;
import com.messkhata.ui.adapter.MemberAdapter;
import com.messkhata.ui.viewmodel.AuthViewModel;
import com.messkhata.utils.Constants;

/**
 * Settings Fragment for app settings and member management.
 */
public class SettingsFragment extends Fragment {

    private AuthViewModel viewModel;

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserRole;
    private TextView tvMessJoinCode;
    private RecyclerView rvMembers;
    private MaterialButton btnLogout;
    private MaterialButton btnSyncNow;

    private MemberAdapter memberAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeData();
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvMessJoinCode = view.findViewById(R.id.tvMessJoinCode);
        rvMembers = view.findViewById(R.id.rvMembers);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSyncNow = view.findViewById(R.id.btnSyncNow);

        // Set current user info
        tvUserName.setText(viewModel.getCurrentUserName());
        tvUserEmail.setText(viewModel.getCurrentFirebaseUser() != null ?
                viewModel.getCurrentFirebaseUser().getEmail() : "");

        // Show join code only for admins
        if (viewModel.isAdmin()) {
            tvMessJoinCode.setVisibility(View.VISIBLE);
            // Load join code from database
            loadJoinCode();
        } else {
            tvMessJoinCode.setVisibility(View.GONE);
        }
    }

    private void loadJoinCode() {
        // This would be loaded from the database
        // For now, just show a placeholder
        tvMessJoinCode.setText("Join Code: Loading...");
    }

    private void setupRecyclerView() {
        memberAdapter = new MemberAdapter(new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onMemberClick(User user) {
                // Show member details
            }

            @Override
            public void onChangeRole(User user, String newRole) {
                confirmRoleChange(user, newRole);
            }

            @Override
            public void onRemoveMember(User user) {
                confirmRemoveMember(user);
            }
        }, viewModel.isAdmin());

        rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMembers.setAdapter(memberAdapter);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> confirmLogout());

        btnSyncNow.setOnClickListener(v -> {
            SyncWorker.triggerImmediateSync(requireContext());
            Toast.makeText(getContext(), "Sync started...", Toast.LENGTH_SHORT).show();
        });

        // Copy join code on click
        tvMessJoinCode.setOnClickListener(v -> {
            if (viewModel.isAdmin()) {
                String code = tvMessJoinCode.getText().toString();
                if (code.contains(":")) {
                    code = code.split(":")[1].trim();
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) requireContext()
                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Join Code", code);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "Join code copied!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void observeData() {
        // Observe current user
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                tvUserRole.setText(user.getRole());
            }
        });

        // Observe members
        viewModel.getMessMembers().observe(getViewLifecycleOwner(), members -> {
            memberAdapter.submitList(members);
        });
    }

    private void confirmRoleChange(User user, String newRole) {
        String roleDisplay = Constants.ROLE_MANAGER.equals(newRole) ? "Manager" : "Member";

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Role")
                .setMessage("Make " + user.getName() + " a " + roleDisplay + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    viewModel.updateUserRole(user.getId(), newRole);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmRemoveMember(User user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + user.getName() + " from the mess?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    viewModel.removeUserFromMess(user.getId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        // Stop sync
        SyncWorker.cancelSync(requireContext());

        MessKhataDatabase database = MessKhataDatabase.getInstance(requireContext());
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(database);
        syncManager.stopSync();

        // Sign out
        viewModel.signOut();

        // Navigate to login
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
