package com.messkhata.data.sync;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;

/**
 * Helper class for Firebase Authentication operations
 */
public class FirebaseAuthHelper {

    private static final String TAG = "FirebaseAuthHelper";

    private static FirebaseAuthHelper instance;
    private final FirebaseAuth auth;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onFailure(String error);
    }

    private FirebaseAuthHelper() {
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthHelper();
        }
        return instance;
    }

    /**
     * Get current Firebase Auth instance
     */
    public FirebaseAuth getAuth() {
        return auth;
    }

    /**
     * Check if user is signed in
     */
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Get current user's UID
     */
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Get current user's email
     */
    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Sign up with email and password
     */
    public void signUpWithEmail(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User created successfully");
                        callback.onSuccess(auth.getCurrentUser());
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Sign up failed";
                        Log.e(TAG, "Sign up failed: " + error);
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Sign in with email and password
     */
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User signed in successfully");
                        callback.onSuccess(auth.getCurrentUser());
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Sign in failed";
                        Log.e(TAG, "Sign in failed: " + error);
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Sign out current user
     */
    public void signOut() {
        auth.signOut();
        Log.d(TAG, "User signed out");
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent");
                        callback.onSuccess(null);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Failed to send reset email";
                        Log.e(TAG, "Password reset failed: " + error);
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Update user's display name
     */
    public void updateDisplayName(String displayName, AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No user signed in");
            return;
        }

        com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Display name updated");
                        callback.onSuccess(user);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Failed to update profile";
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Re-authenticate user (required before sensitive operations)
     */
    public void reAuthenticate(String email, String password, AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No user signed in");
            return;
        }

        com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User re-authenticated");
                        callback.onSuccess(user);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Re-authentication failed";
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Delete current user account
     */
    public void deleteAccount(AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No user signed in");
            return;
        }

        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User account deleted");
                        callback.onSuccess(null);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Failed to delete account";
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Add auth state listener
     */
    public void addAuthStateListener(FirebaseAuth.AuthStateListener listener) {
        auth.addAuthStateListener(listener);
    }

    /**
     * Remove auth state listener
     */
    public void removeAuthStateListener(FirebaseAuth.AuthStateListener listener) {
        auth.removeAuthStateListener(listener);
    }
}
