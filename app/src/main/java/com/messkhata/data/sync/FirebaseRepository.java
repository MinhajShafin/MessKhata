package com.messkhata.data.sync;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.messkhata.data.sync.model.SyncableExpense;
import com.messkhata.data.sync.model.SyncableMeal;
import com.messkhata.data.sync.model.SyncableMess;
import com.messkhata.data.sync.model.SyncableUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Firebase Firestore operations
 * Handles all cloud database interactions
 */
public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";

    private static FirebaseRepository instance;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final ExecutorService executor;

    private FirebaseRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        executor = Executors.newFixedThreadPool(4);
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }

    /**
     * Check if user is authenticated with Firebase
     */
    public boolean isAuthenticated() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Get current Firebase user
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

    // ==================== MESS OPERATIONS ====================

    /**
     * Save mess to Firestore
     */
    public Task<DocumentReference> saveMess(SyncableMess mess) {
        Map<String, Object> data = mess.toFirebaseMap();

        if (mess.getFirebaseId() != null && !mess.getFirebaseId().isEmpty()) {
            // Update existing document
            return firestore.collection(SyncableMess.COLLECTION_NAME)
                    .document(mess.getFirebaseId())
                    .set(data, SetOptions.merge())
                    .continueWith(task -> firestore.collection(SyncableMess.COLLECTION_NAME)
                            .document(mess.getFirebaseId()));
        } else {
            // Create new document
            return firestore.collection(SyncableMess.COLLECTION_NAME).add(data);
        }
    }

    /**
     * Get mess by local ID
     */
    public Task<SyncableMess> getMessByLocalId(int messId) {
        return firestore.collection(SyncableMess.COLLECTION_NAME)
                .whereEqualTo("messId", messId)
                .limit(1)
                .get()
                .continueWith(task -> {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        return SyncableMess.fromFirebaseMap(doc.getId(), doc.getData());
                    }
                    return null;
                });
    }

    /**
     * Get mess by Firebase ID
     */
    public Task<SyncableMess> getMessByFirebaseId(String firebaseId) {
        return firestore.collection(SyncableMess.COLLECTION_NAME)
                .document(firebaseId)
                .get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        return SyncableMess.fromFirebaseMap(doc.getId(), doc.getData());
                    }
                    return null;
                });
    }

    /**
     * Delete mess from Firestore
     */
    public Task<Void> deleteMess(String firebaseId) {
        return firestore.collection(SyncableMess.COLLECTION_NAME)
                .document(firebaseId)
                .delete();
    }

    // ==================== USER OPERATIONS ====================

    /**
     * Save user to Firestore
     */
    public Task<DocumentReference> saveUser(SyncableUser user) {
        Map<String, Object> data = user.toFirebaseMap();

        if (user.getFirebaseId() != null && !user.getFirebaseId().isEmpty()) {
            return firestore.collection(SyncableUser.COLLECTION_NAME)
                    .document(user.getFirebaseId())
                    .set(data, SetOptions.merge())
                    .continueWith(task -> firestore.collection(SyncableUser.COLLECTION_NAME)
                            .document(user.getFirebaseId()));
        } else {
            return firestore.collection(SyncableUser.COLLECTION_NAME).add(data);
        }
    }

    /**
     * Get user by email
     */
    public Task<SyncableUser> getUserByEmail(String email) {
        return firestore.collection(SyncableUser.COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .continueWith(task -> {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        return SyncableUser.fromFirebaseMap(doc.getId(), doc.getData());
                    }
                    return null;
                });
    }

    /**
     * Get all users for a mess by Firebase mess ID
     */
    public Task<List<SyncableUser>> getUsersByMessId(int messId) {
        // Note: This still uses messId for backward compatibility
        // New method getUsersByFirebaseMessId should be preferred
        return firestore.collection(SyncableUser.COLLECTION_NAME)
                .whereEqualTo("messId", messId)
                .get()
                .continueWith(task -> {
                    List<SyncableUser> users = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            users.add(SyncableUser.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return users;
                });
    }

    /**
     * Get all users for a mess by Firebase mess document ID
     */
    public Task<List<SyncableUser>> getUsersByFirebaseMessId(String firebaseMessId) {
        Log.d(TAG, "getUsersByFirebaseMessId called with: " + firebaseMessId);
        return firestore.collection(SyncableUser.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .get()
                .continueWith(task -> {
                    List<SyncableUser> users = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null) {
                            Log.d(TAG, "Firebase query returned " + snapshot.size() + " documents");
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Log.d(TAG, "User doc: " + doc.getId() + ", data: " + doc.getData());
                                users.add(SyncableUser.fromFirebaseMap(doc.getId(), doc.getData()));
                            }
                        } else {
                            Log.d(TAG, "Firebase query returned null snapshot");
                        }
                    } else {
                        Log.e(TAG, "Firebase query failed", task.getException());
                    }
                    return users;
                });
    }

    /**
     * Delete user from Firestore
     */
    public Task<Void> deleteUser(String firebaseId) {
        return firestore.collection(SyncableUser.COLLECTION_NAME)
                .document(firebaseId)
                .delete();
    }

    // ==================== MEAL OPERATIONS ====================

    /**
     * Save meal to Firestore
     */
    public Task<DocumentReference> saveMeal(SyncableMeal meal) {
        Map<String, Object> data = meal.toFirebaseMap();

        if (meal.getFirebaseId() != null && !meal.getFirebaseId().isEmpty()) {
            return firestore.collection(SyncableMeal.COLLECTION_NAME)
                    .document(meal.getFirebaseId())
                    .set(data, SetOptions.merge())
                    .continueWith(task -> firestore.collection(SyncableMeal.COLLECTION_NAME)
                            .document(meal.getFirebaseId()));
        } else {
            return firestore.collection(SyncableMeal.COLLECTION_NAME).add(data);
        }
    }

    /**
     * Get meals for a user in a mess
     */
    public Task<List<SyncableMeal>> getMealsByUserAndMess(int userId, int messId) {
        return firestore.collection(SyncableMeal.COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("messId", messId)
                .get()
                .continueWith(task -> {
                    List<SyncableMeal> meals = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            meals.add(SyncableMeal.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return meals;
                });
    }

    /**
     * Get all meals for a mess
     */
    public Task<List<SyncableMeal>> getMealsByMessId(int messId) {
        return firestore.collection(SyncableMeal.COLLECTION_NAME)
                .whereEqualTo("messId", messId)
                .get()
                .continueWith(task -> {
                    List<SyncableMeal> meals = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            meals.add(SyncableMeal.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return meals;
                });
    }

    /**
     * Get ALL meals for a mess by Firebase mess ID (no timestamp filter - doesn't
     * require index)
     */
    public Task<List<SyncableMeal>> getAllMealsByFirebaseMessId(String firebaseMessId) {
        Log.d(TAG, "getAllMealsByFirebaseMessId called with: " + firebaseMessId);
        return firestore.collection(SyncableMeal.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .get()
                .continueWith(task -> {
                    List<SyncableMeal> meals = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null) {
                            Log.d(TAG, "Firebase returned " + snapshot.size() + " meals");
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                meals.add(SyncableMeal.fromFirebaseMap(doc.getId(), doc.getData()));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting meals", task.getException());
                    }
                    return meals;
                });
    }

    /**
     * Get meals modified after a certain timestamp (by Firebase mess ID)
     */
    public Task<List<SyncableMeal>> getMealsModifiedAfter(String firebaseMessId, long timestamp) {
        return firestore.collection(SyncableMeal.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .whereGreaterThan("lastModified", timestamp)
                .get()
                .continueWith(task -> {
                    List<SyncableMeal> meals = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            meals.add(SyncableMeal.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return meals;
                });
    }

    /**
     * Get meals modified after a certain timestamp (deprecated - uses local messId)
     */
    @Deprecated
    public Task<List<SyncableMeal>> getMealsModifiedAfter(int messId, long timestamp) {
        return firestore.collection(SyncableMeal.COLLECTION_NAME)
                .whereEqualTo("messId", messId)
                .whereGreaterThan("lastModified", timestamp)
                .get()
                .continueWith(task -> {
                    List<SyncableMeal> meals = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            meals.add(SyncableMeal.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return meals;
                });
    }

    /**
     * Delete meal from Firestore
     */
    public Task<Void> deleteMeal(String firebaseId) {
        return firestore.collection(SyncableMeal.COLLECTION_NAME)
                .document(firebaseId)
                .delete();
    }

    // ==================== EXPENSE OPERATIONS ====================

    /**
     * Save expense to Firestore
     */
    public Task<DocumentReference> saveExpense(SyncableExpense expense) {
        Map<String, Object> data = expense.toFirebaseMap();

        if (expense.getFirebaseId() != null && !expense.getFirebaseId().isEmpty()) {
            return firestore.collection(SyncableExpense.COLLECTION_NAME)
                    .document(expense.getFirebaseId())
                    .set(data, SetOptions.merge())
                    .continueWith(task -> firestore.collection(SyncableExpense.COLLECTION_NAME)
                            .document(expense.getFirebaseId()));
        } else {
            return firestore.collection(SyncableExpense.COLLECTION_NAME).add(data);
        }
    }

    /**
     * Get all expenses for a mess
     */
    public Task<List<SyncableExpense>> getExpensesByMessId(int messId) {
        return firestore.collection(SyncableExpense.COLLECTION_NAME)
                .whereEqualTo("messId", messId)
                .orderBy("expenseDate", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<SyncableExpense> expenses = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            expenses.add(SyncableExpense.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return expenses;
                });
    }

    /**
     * Get ALL expenses for a mess by Firebase mess ID (no timestamp filter -
     * doesn't require index)
     */
    public Task<List<SyncableExpense>> getAllExpensesByFirebaseMessId(String firebaseMessId) {
        Log.d(TAG, "getAllExpensesByFirebaseMessId called with: " + firebaseMessId);
        return firestore.collection(SyncableExpense.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .get()
                .continueWith(task -> {
                    List<SyncableExpense> expenses = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null) {
                            Log.d(TAG, "Firebase returned " + snapshot.size() + " expenses");
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                expenses.add(SyncableExpense.fromFirebaseMap(doc.getId(), doc.getData()));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting expenses", task.getException());
                    }
                    return expenses;
                });
    }

    /**
     * Get expenses modified after a certain timestamp (by Firebase mess ID)
     */
    public Task<List<SyncableExpense>> getExpensesModifiedAfter(String firebaseMessId, long timestamp) {
        return firestore.collection(SyncableExpense.COLLECTION_NAME)
                .whereEqualTo("firebaseMessId", firebaseMessId)
                .whereGreaterThan("lastModified", timestamp)
                .get()
                .continueWith(task -> {
                    List<SyncableExpense> expenses = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            expenses.add(SyncableExpense.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return expenses;
                });
    }

    /**
     * Get expenses modified after a certain timestamp (deprecated - uses local
     * messId)
     */
    @Deprecated
    public Task<List<SyncableExpense>> getExpensesModifiedAfter(int messId, long timestamp) {
        return firestore.collection(SyncableExpense.COLLECTION_NAME)
                .whereEqualTo("messId", messId)
                .whereGreaterThan("lastModified", timestamp)
                .get()
                .continueWith(task -> {
                    List<SyncableExpense> expenses = new ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            expenses.add(SyncableExpense.fromFirebaseMap(doc.getId(), doc.getData()));
                        }
                    }
                    return expenses;
                });
    }

    /**
     * Delete expense from Firestore
     */
    public Task<Void> deleteExpense(String firebaseId) {
        return firestore.collection(SyncableExpense.COLLECTION_NAME)
                .document(firebaseId)
                .delete();
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * Save multiple meals in a batch
     */
    public Task<Void> saveMealsBatch(List<SyncableMeal> meals) {
        WriteBatch batch = firestore.batch();

        for (SyncableMeal meal : meals) {
            DocumentReference ref;
            if (meal.getFirebaseId() != null && !meal.getFirebaseId().isEmpty()) {
                ref = firestore.collection(SyncableMeal.COLLECTION_NAME)
                        .document(meal.getFirebaseId());
            } else {
                ref = firestore.collection(SyncableMeal.COLLECTION_NAME).document();
            }
            batch.set(ref, meal.toFirebaseMap(), SetOptions.merge());
        }

        return batch.commit();
    }

    /**
     * Save multiple expenses in a batch
     */
    public Task<Void> saveExpensesBatch(List<SyncableExpense> expenses) {
        WriteBatch batch = firestore.batch();

        for (SyncableExpense expense : expenses) {
            DocumentReference ref;
            if (expense.getFirebaseId() != null && !expense.getFirebaseId().isEmpty()) {
                ref = firestore.collection(SyncableExpense.COLLECTION_NAME)
                        .document(expense.getFirebaseId());
            } else {
                ref = firestore.collection(SyncableExpense.COLLECTION_NAME).document();
            }
            batch.set(ref, expense.toFirebaseMap(), SetOptions.merge());
        }

        return batch.commit();
    }

    /**
     * Enable offline persistence (called once on app start)
     */
    public void enableOfflinePersistence() {
        firestore.getFirestoreSettings();
        // Offline persistence is enabled by default in Firebase SDK 21+
        Log.d(TAG, "Firestore offline persistence enabled");
    }
}
