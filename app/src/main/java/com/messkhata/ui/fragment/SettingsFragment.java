package com.messkhata.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.messkhata.R;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Mess;
import com.messkhata.data.model.User;
import com.messkhata.data.sync.FirebaseAuthHelper;
import com.messkhata.data.sync.SyncManager;
import com.messkhata.data.sync.SyncWorker;
import com.messkhata.ui.activity.LoginActivity;
import com.messkhata.ui.activity.MessSetupActivity;
import com.messkhata.ui.adapter.MemberAdapter;
import com.messkhata.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings Fragment - User and mess settings
 */
public class SettingsFragment extends Fragment implements MemberAdapter.OnMemberActionListener {

    // UI Components - User Profile
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserRole;

    // UI Components - Mess Info
    private TextView tvMessName;
    private TextView tvMessJoinCode;
    private View cardMemberManagement;
    private RecyclerView rvMembers;

    // UI Components - Actions
    private MaterialButton btnLogout;
    private MaterialButton btnLeaveMess;
    private View layoutSync;
    private View progressSync;

    // Adapter
    private MemberAdapter memberAdapter;

    // DAOs
    private UserDao userDao;
    private MessDao messDao;
    private SyncManager syncManager;

    // Session data
    private PreferenceManager prefManager;
    private long userId;
    private int messId;
    private String userRole;

    // Member list
    private List<User> memberList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeDAOs();
        loadSessionData();
        setupListeners();
        loadUserProfile();
        loadMessInfo();
        loadMembers();
    }

    private void initializeViews(View view) {
        // User profile
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserRole = view.findViewById(R.id.tvUserRole);

        // Mess info
        tvMessName = view.findViewById(R.id.tvMessName);
        tvMessJoinCode = view.findViewById(R.id.tvMessJoinCode);
        cardMemberManagement = view.findViewById(R.id.cardMemberManagement);
        rvMembers = view.findViewById(R.id.rvMembers);

        // Setup RecyclerView
        if (rvMembers != null) {
            rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // Actions
        btnLogout = view.findViewById(R.id.btnLogout);
        btnLeaveMess = view.findViewById(R.id.btnLeaveMess);
        layoutSync = view.findViewById(R.id.layoutSync);
        progressSync = view.findViewById(R.id.progressSync);
    }

    private void initializeDAOs() {
        userDao = new UserDao(requireContext());
        messDao = new MessDao(requireContext());
        syncManager = SyncManager.getInstance(requireContext());
    }

    private void loadSessionData() {
        prefManager = PreferenceManager.getInstance(requireContext());

        // Check if session exists
        String userIdStr = prefManager.getUserId();
        String messIdStr = prefManager.getMessId();
        String userRoleStr = prefManager.getUserRole();

        if (userIdStr == null || messIdStr == null) {
            Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        userId = Long.parseLong(userIdStr);
        messId = Integer.parseInt(messIdStr);
        userRole = userRoleStr != null ? userRoleStr : "MEMBER";

        // Initialize adapter
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
        memberAdapter = new MemberAdapter(memberList, isAdmin, this);
        if (rvMembers != null) {
            rvMembers.setAdapter(memberAdapter);
        }
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> logout());

        if (btnLeaveMess != null) {
            btnLeaveMess.setOnClickListener(v -> showLeaveMessConfirmation());
        }

        if (layoutSync != null) {
            layoutSync.setOnClickListener(v -> triggerManualSync());
        }
    }

    private void triggerManualSync() {
        if (progressSync != null) {
            progressSync.setVisibility(View.VISIBLE);
        }

        Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show();

        // Trigger sync in background
        SyncWorker.triggerImmediateSync(requireContext());

        // Also perform sync directly for immediate feedback
        syncManager.performFullSync(messId);

        // Refresh data after a delay to allow sync to complete
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (progressSync != null) {
                progressSync.setVisibility(View.GONE);
            }
            loadUserProfile();
            loadMessInfo();
            loadMembers();
            Toast.makeText(requireContext(), "Sync complete!", Toast.LENGTH_SHORT).show();
        }, 3000);
    }

    private void showLeaveMessConfirmation() {
        // Check if user is admin
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            // Admins cannot leave - they need to transfer ownership or delete the mess
            Toast.makeText(requireContext(),
                    "As an admin, you cannot leave the mess. Transfer ownership first or delete the mess.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.leave_mess)
                .setMessage(R.string.leave_mess_confirm)
                .setPositiveButton("Leave", (dialog, which) -> leaveMess())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveMess() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Update user's messId to -1 (no mess)
                userDao.updateUserMessId(userId, -1);
                userDao.updateUserRole(userId, "member");

                // Get updated user and sync to Firebase
                User user = userDao.getUserByIdAsObject((int) userId);
                if (user != null) {
                    syncManager.syncUserImmediate(user);
                }

                requireActivity().runOnUiThread(() -> {
                    // Clear mess from session but keep user logged in
                    prefManager.clearMessSession();

                    Toast.makeText(requireContext(), "You have left the mess", Toast.LENGTH_SHORT).show();

                    // Navigate to MessSetupActivity
                    Intent intent = new Intent(requireContext(), MessSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to leave mess: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadUserProfile() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                User user = userDao.getUserByIdAsObject((int) userId);

                requireActivity().runOnUiThread(() -> {
                    if (user != null) {
                        tvUserName.setText(user.getFullName());
                        tvUserEmail.setText(user.getEmail());
                        tvUserRole.setText(user.getRole());

                        // Show/hide admin settings based on role
                        if ("ADMIN".equals(user.getRole())) {
                            cardMemberManagement.setVisibility(View.VISIBLE);
                        } else {
                            cardMemberManagement.setVisibility(View.GONE);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadMessInfo() {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Mess mess = messDao.getMessByIdAsObject(messId);
                // Get the Firebase invitation code (6-digit) instead of local code
                String invitationCode = messDao.getSavedInvitationCode(messId);

                requireActivity().runOnUiThread(() -> {
                    if (mess != null) {
                        tvMessName.setText(mess.getMessName());
                        tvMessJoinCode.setText(invitationCode);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void logout() {
        // Sign out from Firebase
        FirebaseAuthHelper.getInstance().signOut();

        // Cancel background sync
        SyncWorker.cancelPeriodicSync(requireContext());

        // Clear preferences
        prefManager.clearSession();

        // Navigate to login
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadMembers() {
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return; // Only admins can see member list
        }

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<User> members = userDao.getMembersByMessId(messId);

                requireActivity().runOnUiThread(() -> {
                    memberList.clear();
                    if (members != null) {
                        memberList.addAll(members);
                    }
                    memberAdapter.updateMembers(memberList);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onMemberMoreOptions(User user) {
        // TODO: Show popup menu with options (promote/demote, remove member)
        Toast.makeText(requireContext(), "Options for: " + user.getFullName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        loadMessInfo();
        loadMembers();
    }
}
