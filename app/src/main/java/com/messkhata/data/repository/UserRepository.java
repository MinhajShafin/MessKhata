package com.messkhata.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.messkhata.data.dao.MessDao;
import com.messkhata.data.dao.UserDao;
import com.messkhata.data.database.MessKhataDatabase;
import com.messkhata.data.model.Mess;
import com.messkhata.data.model.User;
import com.messkhata.sync.FirebaseSyncManager;
import com.messkhata.utils.Constants;
import com.messkhata.utils.IdGenerator;
import com.messkhata.utils.NetworkUtils;
import com.messkhata.utils.PreferenceManager;

import java.util.List;

/**
 * Repository for User and Authentication operations.
 */
public class UserRepository {

    private final UserDao userDao;
    private final MessDao messDao;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseSyncManager syncManager;
    private final NetworkUtils networkUtils;
    private final PreferenceManager prefManager;
    private final Application application;

    private final MutableLiveData<AuthResult> authResult = new MutableLiveData<>();

    public UserRepository(Application application) {
        this.application = application;
        MessKhataDatabase database = MessKhataDatabase.getInstance(application);
        userDao = database.userDao();
        messDao = database.messDao();
        firebaseAuth = FirebaseAuth.getInstance();
        syncManager = FirebaseSyncManager.getInstance(database);
        networkUtils = NetworkUtils.getInstance(application);
        prefManager = PreferenceManager.getInstance(application);
    }

    /**
     * Get current Firebase user.
     */
    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Get current user from local database.
     */
    public LiveData<User> getCurrentUser() {
        String userId = prefManager.getUserId();
        return userDao.getUserById(userId);
    }

    /**
     * Get all active members of the mess.
     */
    public LiveData<List<User>> getMessMembers() {
        String messId = prefManager.getMessId();
        return userDao.getActiveUsersByMess(messId);
    }

    /**
     * Get active member count.
     */
    public int getActiveMemberCount() {
        String messId = prefManager.getMessId();
        return userDao.getActiveMemberCount(messId);
    }

    /**
     * Sign up a new user.
     */
    public void signUp(String email, String password, String name, String phone) {
        if (!networkUtils.isOnline()) {
            authResult.postValue(new AuthResult(false, "No internet connection"));
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResultData -> {
                    FirebaseUser firebaseUser = authResultData.getUser();
                    if (firebaseUser != null) {
                        createLocalUser(firebaseUser.getUid(), email, name, phone, null);
                    }
                })
                .addOnFailureListener(e -> {
                    authResult.postValue(new AuthResult(false, e.getMessage()));
                });
    }

    /**
     * Sign in existing user.
     */
    public void signIn(String email, String password) {
        if (!networkUtils.isOnline()) {
            // Try offline login
            attemptOfflineLogin(email);
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResultData -> {
                    FirebaseUser firebaseUser = authResultData.getUser();
                    if (firebaseUser != null) {
                        loadUserFromFirebase(firebaseUser.getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    authResult.postValue(new AuthResult(false, e.getMessage()));
                });
    }

    /**
     * Sign out current user.
     */
    public void signOut() {
        firebaseAuth.signOut();
        prefManager.clearSession();
        syncManager.stopSync();
    }

    /**
     * Create a new mess and set user as admin.
     */
    public void createMess(String messName, String address) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            String userId = prefManager.getUserId();
            String messId = IdGenerator.generateId();

            // Create mess
            Mess mess = new Mess(messId, messName, address, userId);
            messDao.insert(mess);

            // Update user with mess ID and admin role
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                user.setMessId(messId);
                user.setRole(Constants.ROLE_ADMIN);
                user.setUpdatedAt(System.currentTimeMillis());
                user.setSynced(false);
                user.setPendingAction(Constants.ACTION_UPDATE);
                userDao.update(user);

                // Update preferences
                prefManager.setMessId(messId);
                prefManager.setUserRole(Constants.ROLE_ADMIN);
            }

            // Sync with Firebase
            if (networkUtils.isOnline()) {
                syncManager.createMess(mess, new FirebaseSyncManager.OnSyncCompleteListener() {
                    @Override
                    public void onSuccess() {
                        syncManager.uploadPendingChanges();
                        syncManager.initSync(messId, userId);
                    }

                    @Override
                    public void onFailure(String error) {
                        // Mess will be synced later
                    }
                });
            }

            authResult.postValue(new AuthResult(true, "Mess created successfully"));
        });
    }

    /**
     * Join an existing mess using join code.
     */
    public void joinMess(String joinCode) {
        if (!networkUtils.isOnline()) {
            authResult.postValue(new AuthResult(false, "No internet connection"));
            return;
        }

        syncManager.findMessByJoinCode(joinCode, new FirebaseSyncManager.OnMessFoundListener() {
            @Override
            public void onFound(Mess mess) {
                MessKhataDatabase.databaseWriteExecutor.execute(() -> {
                    // Save mess locally
                    mess.setSynced(true);
                    messDao.insert(mess);

                    // Update user with mess ID
                    String userId = prefManager.getUserId();
                    User user = userDao.getUserByIdSync(userId);
                    if (user != null) {
                        user.setMessId(mess.getId());
                        user.setRole(Constants.ROLE_MEMBER);
                        user.setUpdatedAt(System.currentTimeMillis());
                        user.setSynced(false);
                        user.setPendingAction(Constants.ACTION_UPDATE);
                        userDao.update(user);

                        // Update preferences
                        prefManager.setMessId(mess.getId());
                        prefManager.setUserRole(Constants.ROLE_MEMBER);

                        // Sync
                        syncManager.uploadPendingChanges();
                        syncManager.initSync(mess.getId(), userId);
                    }

                    authResult.postValue(new AuthResult(true, "Joined mess successfully"));
                });
            }

            @Override
            public void onNotFound() {
                authResult.postValue(new AuthResult(false, "Invalid join code"));
            }

            @Override
            public void onError(String error) {
                authResult.postValue(new AuthResult(false, error));
            }
        });
    }

    /**
     * Update user role (Admin only).
     */
    public void updateUserRole(String userId, String newRole) {
        // Check if current user is admin
        if (!prefManager.isAdmin()) {
            return;
        }

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                user.setRole(newRole);
                user.setUpdatedAt(System.currentTimeMillis());
                user.setSynced(false);
                user.setPendingAction(Constants.ACTION_UPDATE);
                userDao.update(user);

                if (networkUtils.isOnline()) {
                    syncManager.uploadPendingChanges();
                }
            }
        });
    }

    /**
     * Remove user from mess (Admin only).
     */
    public void removeUserFromMess(String userId) {
        // Check if current user is admin
        if (!prefManager.isAdmin()) {
            return;
        }

        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                user.setActive(false);
                user.setUpdatedAt(System.currentTimeMillis());
                user.setSynced(false);
                user.setPendingAction(Constants.ACTION_UPDATE);
                userDao.update(user);

                if (networkUtils.isOnline()) {
                    syncManager.uploadPendingChanges();
                }
            }
        });
    }

    /**
     * Get auth result LiveData.
     */
    public LiveData<AuthResult> getAuthResult() {
        return authResult;
    }

    // ==================== PRIVATE METHODS ====================

    private void createLocalUser(String firebaseUid, String email, String name,
                                  String phone, String messId) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            User user = new User(firebaseUid, email, name, phone,
                                Constants.ROLE_MEMBER, messId);
            userDao.insert(user);

            // Save session
            prefManager.saveUserSession(firebaseUid, messId,
                                        Constants.ROLE_MEMBER, name, email);

            // Sync to Firebase
            if (networkUtils.isOnline()) {
                syncManager.uploadPendingChanges();
            }

            authResult.postValue(new AuthResult(true, "Account created successfully"));
        });
    }

    private void loadUserFromFirebase(String userId) {
        // For now, just set up the session
        // In production, you'd fetch user data from Firebase
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            User localUser = userDao.getUserByIdSync(userId);

            if (localUser != null) {
                // User exists locally
                prefManager.saveUserSession(
                        localUser.getId(),
                        localUser.getMessId(),
                        localUser.getRole(),
                        localUser.getName(),
                        localUser.getEmail()
                );

                if (localUser.getMessId() != null) {
                    syncManager.initSync(localUser.getMessId(), userId);
                }

                authResult.postValue(new AuthResult(true, "Login successful"));
            } else {
                // User doesn't exist locally, create from Firebase auth
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    String email = firebaseUser.getEmail();
                    String name = firebaseUser.getDisplayName();
                    if (name == null) name = email != null ? email.split("@")[0] : "User";

                    User newUser = new User(userId, email, name, "",
                                           Constants.ROLE_MEMBER, null);
                    newUser.setSynced(true); // Assume it exists on server
                    userDao.insert(newUser);

                    prefManager.saveUserSession(userId, null,
                                               Constants.ROLE_MEMBER, name, email);

                    authResult.postValue(new AuthResult(true, "Login successful"));
                }
            }
        });
    }

    private void attemptOfflineLogin(String email) {
        MessKhataDatabase.databaseWriteExecutor.execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (user != null) {
                prefManager.saveUserSession(
                        user.getId(),
                        user.getMessId(),
                        user.getRole(),
                        user.getName(),
                        user.getEmail()
                );
                authResult.postValue(new AuthResult(true, "Offline login successful"));
            } else {
                authResult.postValue(new AuthResult(false,
                        "No offline data available. Please connect to the internet."));
            }
        });
    }

    /**
     * Auth result wrapper class.
     */
    public static class AuthResult {
        public final boolean success;
        public final String message;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
