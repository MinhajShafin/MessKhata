package com.messkhata.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseUser;
import com.messkhata.data.model.User;
import com.messkhata.data.repository.UserRepository;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * ViewModel for Authentication and User management.
 */
public class AuthViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final PreferenceManager prefManager;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return prefManager.isLoggedIn();
    }

    /**
     * Check if user has joined a mess.
     */
    public boolean hasJoinedMess() {
        return prefManager.getMessId() != null;
    }

    /**
     * Get current Firebase user.
     */
    public FirebaseUser getCurrentFirebaseUser() {
        return userRepository.getCurrentFirebaseUser();
    }

    /**
     * Get current user from database.
     */
    public LiveData<User> getCurrentUser() {
        return userRepository.getCurrentUser();
    }

    /**
     * Get all mess members.
     */
    public LiveData<List<User>> getMessMembers() {
        return userRepository.getMessMembers();
    }

    /**
     * Sign up new user.
     */
    public void signUp(String email, String password, String name, String phone) {
        userRepository.signUp(email, password, name, phone);
    }

    /**
     * Sign in existing user.
     */
    public void signIn(String email, String password) {
        userRepository.signIn(email, password);
    }

    /**
     * Sign out current user.
     */
    public void signOut() {
        userRepository.signOut();
    }

    /**
     * Create a new mess.
     */
    public void createMess(String name, String address) {
        userRepository.createMess(name, address);
    }

    /**
     * Join existing mess with code.
     */
    public void joinMess(String joinCode) {
        userRepository.joinMess(joinCode);
    }

    /**
     * Get authentication result.
     */
    public LiveData<UserRepository.AuthResult> getAuthResult() {
        return userRepository.getAuthResult();
    }

    /**
     * Update user role (Admin only).
     */
    public void updateUserRole(String userId, String newRole) {
        userRepository.updateUserRole(userId, newRole);
    }

    /**
     * Remove user from mess (Admin only).
     */
    public void removeUserFromMess(String userId) {
        userRepository.removeUserFromMess(userId);
    }

    /**
     * Check if current user is admin.
     */
    public boolean isAdmin() {
        return prefManager.isAdmin();
    }

    /**
     * Check if current user is admin or manager.
     */
    public boolean isAdminOrManager() {
        return prefManager.isAdminOrManager();
    }

    /**
     * Get current user ID.
     */
    public String getCurrentUserId() {
        return prefManager.getUserId();
    }

    /**
     * Get current user name.
     */
    public String getCurrentUserName() {
        return prefManager.getUserName();
    }

    /**
     * Get current mess ID.
     */
    public String getCurrentMessId() {
        return prefManager.getMessId();
    }
}
