# MessKhata - Complete Implementation Plan

## Current Status Analysis

### ✅ What's Working:
1. Database structure created (MessKhataDatabase.java)
2. UserDao with basic login/signup
3. LoginActivity & SignUpActivity functional
4. SharedPreferences for session management
5. Basic MessSetupActivity skeleton

### ❌ Critical Issues Found:

1. **Login always redirects to MessSetupActivity** - doesn't check if user already has a mess
2. **Database missing tables:** MealPreferences table not created
3. **Database missing fields:** 
   - Mess table missing `invitationCode` field
   - MonthlyStats missing `totalGas`, `totalRent` fields
   - MonthlyBills missing `gasShare`, `rentShare` fields
4. **No MessDao** - Can't create/join mess
5. **No navigation logic** - Can't determine where to send user after login
6. **UserDao doesn't return messId** after login

---

## Implementation Plan (Minimal & Complete)

### **Phase 1: Database Fixes** ⚡

#### 1.1 Update Database Schema (MessKhataDatabase.java) (Implemented)

**Changes Required:**
- Increment database version from 1 to 2
- **Use messId as invitation code** (simpler approach - no separate field needed)
- Add `totalGas`, `totalRent` fields to MessMonthlyStats table
- Add `gasShare`, `rentShare` fields to MonthlyBills table
- Create new MealPreferences table

**Design Decision: Use messId as Invitation Code**

**Why this is better:**
- ✅ **Simpler:** No need to generate/validate separate codes
- ✅ **No collisions:** Auto-increment guarantees uniqueness
- ✅ **Easier to maintain:** One less field to manage
- ✅ **User-friendly:** Can format as "MK-1001" for display
- ✅ **Future-proof:** Easy to extend (e.g., add prefix for different mess types)

**How it works:**
- messId starts from 1, but we display it starting from 1000 (e.g., 1 → 1000, 2 → 1001)
- Format for users: "1000", "1001", "1002"... (clean 4-digit codes)
- In database: Store as 1, 2, 3... (normal auto-increment)
- Code conversion: `displayCode = messId + 999` and `messId = inputCode - 999`

**Updated SQL Statements:**

```java
// Mess Table - NO invitationCode field needed
private static final String CREATE_MESS_TABLE =
        "CREATE TABLE " + TABLE_MESS + " (" +
                "messId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "messName TEXT NOT NULL, " +
                "groceryBudgetPerMeal REAL NOT NULL DEFAULT 40.00, " +
                "cookingChargePerMeal REAL NOT NULL DEFAULT 10.00, " +
                "createdDate INTEGER DEFAULT (strftime('%s', 'now')), " +
                "isActive INTEGER DEFAULT 1)";

// MessMonthlyStats Table - ADD totalGas, totalRent
private static final String CREATE_MONTHLY_STATS_TABLE =
        "CREATE TABLE " + TABLE_MONTHLY_STATS + " (" +
                "statsId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "messId INTEGER NOT NULL, " +
                "month INTEGER NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "totalGrocery REAL DEFAULT 0.00, " +
                "totalUtilities REAL DEFAULT 0.00, " +
                "totalCleaning REAL DEFAULT 0.00, " +
                "totalGas REAL DEFAULT 0.00, " +           // NEW FIELD
                "totalRent REAL DEFAULT 0.00, " +          // NEW FIELD
                "totalMiscellaneous REAL DEFAULT 0.00, " +
                "totalMeals INTEGER DEFAULT 0, " +
                "numberOfMembers INTEGER NOT NULL, " +
                "cookingCharge REAL NOT NULL, " +
                "isFinalized INTEGER DEFAULT 0, " +
                "finalizedDate INTEGER, " +
                "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                "UNIQUE(messId, month, year))";

// MonthlyBills Table - ADD gasShare, rentShare
private static final String CREATE_MONTHLY_BILLS_TABLE =
        "CREATE TABLE " + TABLE_MONTHLY_BILLS + " (" +
                "billId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER NOT NULL, " +
                "messId INTEGER NOT NULL, " +
                "month INTEGER NOT NULL, " +
                "year INTEGER NOT NULL, " +
                "totalMeals INTEGER NOT NULL, " +
                "mealRate REAL NOT NULL, " +
                "utilitiesShare REAL DEFAULT 0.00, " +
                "cleaningShare REAL DEFAULT 0.00, " +
                "gasShare REAL DEFAULT 0.00, " +            // NEW FIELD
                "rentShare REAL DEFAULT 0.00, " +           // NEW FIELD
                "miscShare REAL DEFAULT 0.00, " +
                "totalPaid REAL DEFAULT 0.00, " +
                "status TEXT DEFAULT 'pending', " +
                "finalizedDate INTEGER, " +
                "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                "updatedAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                "FOREIGN KEY (userId) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE, " +
                "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE, " +
                "UNIQUE(userId, messId, month, year))";

// NEW TABLE: MealPreferences
public static final String TABLE_MEAL_PREFERENCES = "MealPreferences";

private static final String CREATE_MEAL_PREFERENCES_TABLE =
        "CREATE TABLE " + TABLE_MEAL_PREFERENCES + " (" +
                "preferenceId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER NOT NULL, " +
                "messId INTEGER NOT NULL, " +
                "breakfast INTEGER DEFAULT 1, " +
                "lunch INTEGER DEFAULT 1, " +
                "dinner INTEGER DEFAULT 1, " +
                "effectiveFrom INTEGER NOT NULL, " +
                "createdAt INTEGER DEFAULT (strftime('%s', 'now')), " +
                "FOREIGN KEY (userId) REFERENCES " + TABLE_USERS + "(userId) ON DELETE CASCADE, " +
                "FOREIGN KEY (messId) REFERENCES " + TABLE_MESS + "(messId) ON DELETE CASCADE)";
```

**Database Version Update:**
```java
private static final int DATABASE_VERSION = 2;  // Changed from 1 to 2
```

---

#### 1.2 Create MessDao.java (Implemented)

**Location:** `/app/src/main/java/com/messkhata/data/dao/MessDao.java`

**Purpose:** Handle all mess-related database operations

**Required Methods:**

```java
package com.messkhata.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.messkhata.data.database.MessKhataDatabase;

public class MessDao {
    
    private MessKhataDatabase dbHelper;
    
    public MessDao(Context context) {
        this.dbHelper = MessKhataDatabase.getInstance(context);
    }
    
    /**
     * Convert messId to display invitation code
     * messId 1 → code "1000"
     * messId 2 → code "1001"
     */
    public String getInvitationCode(int messId) {
        return String.valueOf(messId + 999);
    }
    
    /**
     * Convert invitation code to messId
     * code "1000" → messId 1
     * code "1001" → messId 2
     */
    public int getMessIdFromCode(String invitationCode) {
        try {
            int code = Integer.parseInt(invitationCode);
            return code - 999;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Create new mess
     * @return messId if successful, -1 if failed
     */
    public long createMess(String messName, double groceryBudget, 
                          double cookingCharge, long creatorUserId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        try {
            // Insert mess (messId auto-generated)
            ContentValues messValues = new ContentValues();
            messValues.put("messName", messName);
            messValues.put("groceryBudgetPerMeal", groceryBudget);
            messValues.put("cookingChargePerMeal", cookingCharge);
            messValues.put("isActive", 1);
            messValues.put("createdDate", System.currentTimeMillis() / 1000);
            
            long messId = db.insert(MessKhataDatabase.TABLE_MESS, null, messValues);
            
            if (messId != -1) {
                // Update creator's messId and role to admin
                ContentValues userValues = new ContentValues();
                userValues.put("messId", messId);
                userValues.put("role", "admin");
                
                db.update(MessKhataDatabase.TABLE_USERS, 
                         userValues, 
                         "userId = ?", 
                         new String[]{String.valueOf(creatorUserId)});
            }
            
            return messId;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Join existing mess using invitation code
     * @return true if successful, false if failed
     */
    public boolean joinMess(String invitationCode, long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        try {
            // Convert code to messId
            int messId = getMessIdFromCode(invitationCode);
            
            if (messId <= 0) {
                return false; // Invalid code format
            }
            
            // Check if mess exists
            String query = "SELECT messId FROM " + MessKhataDatabase.TABLE_MESS + 
                          " WHERE messId = ? AND isActive = 1";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});
            
            if (cursor.moveToFirst()) {
                cursor.close();
                
                // Update user's messId and role to member
                ContentValues values = new ContentValues();
                values.put("messId", messId);
                values.put("role", "member");
                
                int rows = db.update(MessKhataDatabase.TABLE_USERS, 
                                    values, 
                                    "userId = ?", 
                                    new String[]{String.valueOf(userId)});
                return rows > 0;
            }
            
            cursor.close();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get mess by invitation code
     */
    public Cursor getMessByCode(String invitationCode) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int messId = getMessIdFromCode(invitationCode);
        
        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MESS + 
                      " WHERE messId = ? AND isActive = 1";
        return db.rawQuery(query, new String[]{String.valueOf(messId)});
    }
    
    /**
     * Get mess by ID
     */
    public Cursor getMessById(int messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + MessKhataDatabase.TABLE_MESS + 
                      " WHERE messId = ?";
        return db.rawQuery(query, new String[]{String.valueOf(messId)});
    }
    
    /**
     * Update mess rates
     */
    public boolean updateMessRates(int messId, double groceryBudget, double cookingCharge) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("groceryBudgetPerMeal", groceryBudget);
        values.put("cookingChargePerMeal", cookingCharge);
        
        int rows = db.update(MessKhataDatabase.TABLE_MESS, 
                            values, 
                            "messId = ?", 
                            new String[]{String.valueOf(messId)});
        return rows > 0;
    }
}
```

---

### **Phase 2: Fix Authentication Flow** 🔐

#### 2.1 Update UserDao.java (Implemented)

**Add new method to check user's mess status:**

```java
/**
 * Get user's mess ID
 * @return messId if user has mess, -1 if no mess
 */
public int getUserMessId(long userId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    
    String query = "SELECT messId FROM " + MessKhataDatabase.TABLE_USERS + 
                  " WHERE userId = ?";
    Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
    
    int messId = -1;
    if (cursor.moveToFirst()) {
        if (!cursor.isNull(0)) {
            messId = cursor.getInt(0);
        }
    }
    cursor.close();
    return messId;
}

/**
 * Get user's complete info including mess details
 */
public Cursor getUserWithMess(long userId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    
    String query = "SELECT u.*, m.messName, m.invitationCode " +
                  "FROM " + MessKhataDatabase.TABLE_USERS + " u " +
                  "LEFT JOIN " + MessKhataDatabase.TABLE_MESS + " m " +
                  "ON u.messId = m.messId " +
                  "WHERE u.userId = ?";
    return db.rawQuery(query, new String[]{String.valueOf(userId)});
}
```

---

#### 2.2 Fix LoginActivity.java 

**Replace the navigation logic after successful login:**

**Current Code (lines ~100-110):**
```java
if (userId != -1) {
    // Login successful - save user session
    saveUserSession(userId, email);
    
    Toast.makeText(LoginActivity.this,
            "Login successful!",
            Toast.LENGTH_SHORT).show();
    
    // Navigate to MessSetupActivity
    Intent intent = new Intent(LoginActivity.this, MessSetupActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

**New Code:**
```java
if (userId != -1) {
    // Check if user already has a mess
    int messId = userDao.getUserMessId(userId);
    
    // Save user session with mess info
    saveUserSession(userId, email, messId);
    
    Toast.makeText(LoginActivity.this,
            "Login successful!",
            Toast.LENGTH_SHORT).show();
    
    Intent intent;
    if (messId != -1) {
        // User already in mess - go to MainActivity
        intent = new Intent(LoginActivity.this, MainActivity.class);
    } else {
        // User needs to create/join mess
        intent = new Intent(LoginActivity.this, MessSetupActivity.class);
    }
    
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

**Update saveUserSession method:**
```java
private void saveUserSession(long userId, String email, int messId) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putLong("userId", userId);
    editor.putString("userEmail", email);
    editor.putInt("messId", messId);
    editor.putBoolean("isLoggedIn", true);
    editor.apply();
}
```

---

### **Phase 3: Complete MessSetupActivity** 🏠

#### 3.1 Add required fields and DAO

**Add to MessSetupActivity.java:**

```java
private MessDao messDao;
private UserDao userDao;
private SharedPreferences sharedPreferences;
private long userId;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mess_setup);
    
    // Initialize DAOs
    messDao = new MessDao(this);
    userDao = new UserDao(this);
    
    // Get SharedPreferences and userId
    sharedPreferences = getSharedPreferences("MessKhataPrefs", MODE_PRIVATE);
    userId = sharedPreferences.getLong("userId", -1);
    
    if (userId == -1) {
        // Session invalid - go to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
        return;
    }
    
    initViews();
    setupListeners();
}
```

---

#### 3.2 Implement Create Mess

**Replace the create mess click listener:**

```java
btnCreateMess.setOnClickListener(v -> {
    String messName = etMessName.getText().toString().trim();
    
    // Validation
    if (messName.isEmpty()) {
        etMessName.setError("Mess name is required");
        return;
    }
    
    showLoading(true);
    
    // Create mess in background
    MessKhataDatabase.databaseWriteExecutor.execute(() -> {
        // Use default values
        double groceryBudget = 40.00;
        double cookingCharge = 10.00;
        
        long messId = messDao.createMess(messName, groceryBudget, cookingCharge, userId);
        
        runOnUiThread(() -> {
            showLoading(false);
            
            if (messId != -1) {
                // Save messId in SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("messId", (int) messId);
                editor.putString("userRole", "admin");
                editor.apply();
                
                // Get invitation code from messId
                String invitationCode = messDao.getInvitationCode((int) messId);
                
                // Show invitation code
                Toast.makeText(this, 
                    "Mess created! Invitation Code: " + invitationCode, 
                    Toast.LENGTH_LONG).show();
                
                // Navigate to MainActivity
                Intent intent = new Intent(MessSetupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to create mess", Toast.LENGTH_SHORT).show();
            }
        });
    });
});
```

---

#### 3.3 Implement Join Mess

**Replace the join mess click listener:**

```java
btnJoinMess.setOnClickListener(v -> {
    String code = etJoinCode.getText().toString().trim();
    
    // Validation
    if (code.isEmpty()) {
        etJoinCode.setError("Invitation code is required");
        return;
    }
    
    if (code.length() != 4) {
        etJoinCode.setError("Code must be 4 digits");
        return;
    }
    
    showLoading(true);
    
    // Join mess in background
    MessKhataDatabase.databaseWriteExecutor.execute(() -> {
        boolean success = messDao.joinMess(code, userId);
        
        runOnUiThread(() -> {
            showLoading(false);
            
            if (success) {
                // Get messId from database
                int messId = userDao.getUserMessId(userId);
                
                // Save messId in SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("messId", messId);
                editor.putString("userRole", "member");
                editor.apply();
                
                Toast.makeText(this, "Joined mess successfully!", Toast.LENGTH_SHORT).show();
                
                // Navigate to MainActivity
                Intent intent = new Intent(MessSetupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid invitation code", Toast.LENGTH_SHORT).show();
            }
        });
    });
});
```

---

#### 3.4 Add Loading Helper

**Add this method to MessSetupActivity:**

```java
private void showLoading(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    btnCreateMess.setEnabled(!show);
    btnJoinMess.setEnabled(!show);
}
```

---

### **Phase 4: Add Session Check to MainActivity** 🚀

#### 4.1 Add Session Validation

**Add to MainActivity.onCreate() - BEFORE setContentView:**

```java
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
    
    // Rest of your existing code...
}
```

---

### **Phase 5: Update XML Layouts (if needed)** 📱

#### 5.1 Check activity_mess_setup.xml

**Ensure it has these views:**
- `etMessName` - TextInputEditText for mess name
- `etJoinCode` - TextInputEditText for invitation code (4 digits)
- `btnCreateMess` - MaterialButton for creating mess
- `btnJoinMess` - MaterialButton for joining mess
- `progressBar` - ProgressBar for loading state

**Note:** Invitation codes are now 4-digit numbers (e.g., "1000", "1001", "1002"...)

If layout needs updates, we'll handle that separately.

---

## Files Summary

### **Files to Create:**
1. `/app/src/main/java/com/messkhata/data/dao/MessDao.java` ✨ NEW

### **Files to Modify:**
1. `/app/src/main/java/com/messkhata/data/database/MessKhataDatabase.java`
   - Update database version to 2
   - Add totalGas, totalRent to MessMonthlyStats
   - Add gasShare, rentShare to MonthlyBills
   - Create MealPreferences table
   - **Note:** Mess table remains unchanged (no invitationCode field)

2. `/app/src/main/java/com/messkhata/data/dao/UserDao.java`
   - Add `getUserMessId()` method
   - Add `getUserWithMess()` method

3. `/app/src/main/java/com/messkhata/ui/activity/LoginActivity.java`
   - Update navigation logic after login
   - Update `saveUserSession()` to include messId

4. `/app/src/main/java/com/messkhata/ui/activity/MessSetupActivity.java`
   - Add DAO initialization
   - Implement create mess functionality
   - Implement join mess functionality
   - Add loading state management

5. `/app/src/main/java/com/messkhata/MainActivity.java`
   - Add session validation in onCreate()
   - Add navigation logic for unauthenticated users

---

## Testing Checklist

### **Test Scenario 1: New User Registration**
1. ✅ Sign up with new email
2. ✅ Login with credentials
3. ✅ Should redirect to MessSetupActivity
4. ✅ Create new mess
5. ✅ Should navigate to MainActivity
6. ✅ Logout and login again
7. ✅ Should go directly to MainActivity (skip setup)

### **Test Scenario 2: Join Existing Mess**
1. ✅ Sign up with new email
2. ✅ Login with credentials
3. ✅ Should redirect to MessSetupActivity
4. ✅ Enter valid invitation code
5. ✅ Join mess successfully
6. ✅ Should navigate to MainActivity
7. ✅ Logout and login again
8. ✅ Should go directly to MainActivity

### **Test Scenario 3: Invalid Invitation Code**
1. ✅ Go to MessSetupActivity
2. ✅ Enter invalid code (e.g., "9999" - non-existent mess)
3. ✅ Should show "Invalid invitation code" error
4. ✅ Should remain on MessSetupActivity

### **Test Scenario 4: Invitation Code Format**
1. ✅ First mess created - code should be "1000"
2. ✅ Second mess created - code should be "1001"
3. ✅ User can join using "1000" or "1001"
4. ✅ Invalid formats (3 digits, letters) should be rejected

### **Test Scenario 5: App Launch**
1. ✅ Fresh install - should go to LoginActivity
2. ✅ After login without mess - should go to MessSetupActivity
3. ✅ After joining mess - should go to MainActivity
4. ✅ Close and reopen app - should go directly to MainActivity

---

## Implementation Order

### **Step 1:** Database Updates
- Update MessKhataDatabase.java
- Test database creation/migration

### **Step 2:** Create MessDao
- Create MessDao.java file
- Implement code conversion helpers (messId ↔ display code)
- Test code conversion logic

### **Step 3:** Update UserDao
- Add getUserMessId method
- Test with existing users

### **Step 4:** Fix LoginActivity
- Update navigation logic
- Update session management
- Test login flow

### **Step 5:** Complete MessSetupActivity
- Implement create mess
- Implement join mess
- Test both flows

### **Step 6:** Update MainActivity
- Add session checks
- Test app launch scenarios

---

## Next Steps After Implementation

Once this plan is complete, the following features will be ready for implementation:

1. **Dashboard Fragment** - Show user's meals and bills
2. **Meal Entry** - Add daily meal counts
3. **Expense Entry** - Add expenses by category
4. **Admin Features** - Member management, payment recording
5. **Month-End Calculations** - Finalize rates and bills

---

## Notes for University Project

- **Keep it simple:** No complex animations or transitions
- **Focus on functionality:** Core features working correctly
- **Database-first approach:** Ensure data integrity
- **Minimal UI:** Basic Material Design components
- **Clear navigation:** Users should know where they are
- **Error handling:** Show appropriate messages for failures

---

## Estimated Development Time

- **Phase 1 (Database):** 30 minutes
- **Phase 2 (Auth Flow):** 20 minutes
- **Phase 3 (Mess Setup):** 45 minutes
- **Phase 4 (MainActivity):** 15 minutes
- **Phase 5 (Testing):** 30 minutes

**Total:** ~2.5 hours

---

**Ready to implement!** 🚀
