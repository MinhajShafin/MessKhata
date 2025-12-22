# MessKhata - Implementation Plan for Updated Functionality

## Overview
This document outlines the changes needed to implement the new real-time expense tracking system with fixed meal rates and automatic expense distribution.

---

## Key Changes Summary

### Previous Model → New Model
- ❌ **OLD:** Variable meal rate with month-end adjustment
- ✅ **NEW:** Fixed meal rate throughout the month

- ❌ **OLD:** Meal preference applies from tomorrow
- ✅ **NEW:** Meal preference charges TODAY and applies from tomorrow

- ❌ **OLD:** Grocery expenses distributed per meal, others split equally
- ✅ **NEW:** ALL expenses (including grocery) split equally among members

---

## Database Schema Changes

### 1. **Meals Table** - Add New Fields
```sql
ALTER TABLE Meals ADD COLUMN mealRate DECIMAL(10, 2) NOT NULL DEFAULT 50.00;
ALTER TABLE Meals ADD COLUMN mealExpense DECIMAL(10, 2) GENERATED ALWAYS AS (totalMeals * mealRate) STORED;
```

**Changes:**
- Add `mealRate` field to store the fixed rate at time of consumption
- Add `mealExpense` field (auto-calculated: totalMeals × mealRate)

**Impact:**
- `MealDao.java` - Update insert/update queries
- `Meal.java` model - Add new fields

---

### 2. **No PersonalExpenses Table Needed** ✅

**Why not needed:**
- Meal expenses are already tracked in `Meals` table (individual per user)
- Other expenses are in `Expenses` table (shared equally)
- Frontend can calculate shares on-the-fly:
  - **Meal total** = SUM of user's meals × mealRate
  - **Shared total** = (Total expenses ÷ member count)
  - **Grand total** = Meal total + Shared total

**Calculation happens in real-time without storing individual shares**

---

### 3. **MonthlyBills Table** - Simplify Schema
```sql
-- Remove old fields
ALTER TABLE MonthlyBills DROP COLUMN totalMeals;
ALTER TABLE MonthlyBills DROP COLUMN mealRate;
ALTER TABLE MonthlyBills DROP COLUMN mealCost;
ALTER TABLE MonthlyBills DROP COLUMN utilitiesShare;
ALTER TABLE MonthlyBills DROP COLUMN cleaningShare;
ALTER TABLE MonthlyBills DROP COLUMN gasShare;
ALTER TABLE MonthlyBills DROP COLUMN rentShare;
ALTER TABLE MonthlyBills DROP COLUMN miscShare;
ALTER TABLE MonthlyBills DROP COLUMN finalizedDate;

-- Add simplified fields
ALTER TABLE MonthlyBills ADD COLUMN totalMealExpense DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE MonthlyBills ADD COLUMN totalOtherExpenses DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE MonthlyBills MODIFY COLUMN totalBill DECIMAL(10, 2) GENERATED ALWAYS AS (totalMealExpense + totalOtherExpenses) STORED;
ALTER TABLE MonthlyBills MODIFY COLUMN dueAmount DECIMAL(10, 2) GENERATED ALWAYS AS (totalMealExpense + totalOtherExpenses - totalPaid) STORED;
```

**Changes:**
- Simplify to just track totals (calculated from PersonalExpenses)
- Remove individual share fields
- Update `MonthlyBill.java` model
- Update `MonthlyBillDao.java` queries

---

### 4. **MessMonthlyStats Table** - Simplify Schema
```sql
-- Remove calculation fields
ALTER TABLE MessMonthlyStats DROP COLUMN finalGroceryRate;
ALTER TABLE MessMonthlyStats DROP COLUMN cookingCharge;
ALTER TABLE MessMonthlyStats DROP COLUMN finalMealRate;
ALTER TABLE MessMonthlyStats DROP COLUMN isFinalized;
ALTER TABLE MessMonthlyStats DROP COLUMN finalizedDate;

-- Rename and add fields
ALTER TABLE MessMonthlyStats CHANGE COLUMN totalMeals totalMealsConsumed INT DEFAULT 0;
ALTER TABLE MessMonthlyStats ADD COLUMN totalMealExpenses DECIMAL(10, 2) DEFAULT 0.00;
```

**Changes:**
- Remove month-end calculation fields
- Just track totals for reporting
- Update `MessMonthlyStats.java` model
- Update `MessMonthlyStatsDao.java`

---

## Business Logic Changes

### 1. **Meal Preference Logic** - CRITICAL CHANGE

**File:** `MealPreferenceService.java` (or similar)

**OLD Behavior:**
```java
// Preference applies from tomorrow only
void setMealPreference(userId, breakfast, lunch, dinner) {
    preference.effectiveFrom = tomorrow;
    save(preference);
}
```

**NEW Behavior:**
```java
void setMealPreference(userId, breakfast, lunch, dinner) {
    // 1. Check if today's meal already exists
    Meal existingMeal = mealDao.getMealByDate(userId, today);
    
    // 2. Get current meal rate from Mess
    double mealRate = messDao.getMealRate(messId);
    
    // 3. If today NOT already charged, charge NOW
    if (existingMeal == null) {
        Meal todayMeal = new Meal();
        todayMeal.userId = userId;
        todayMeal.mealDate = today;
        todayMeal.breakfast = breakfast;
        todayMeal.lunch = lunch;
        todayMeal.dinner = dinner;
        todayMeal.mealRate = mealRate;
        // mealExpense auto-calculated: totalMeals × mealRate
        mealDao.insert(todayMeal);
        
        // Update MonthlyBill (recalculates from Meals table)
        updateMonthlyBill(userId, getCurrentMonth(), getCurrentYear());
    }
    
    // 4. Save preference for tomorrow onwards
    preference.effectiveFrom = (existingMeal == null) ? tomorrow : today;
    preferenceDao.save(preference);
}
```

**Impact:**
- If today NOT charged → Charge today + save preference for tomorrow
- If today ALREADY charged → Just save preference to start from tomorrow
- Auto-charging continues daily from effective date

---

### 2. **Expense Distribution Logic** - CRITICAL CHANGE

**File:** `ExpenseService.java`

**OLD Behavior:**
```java
void addExpense(messId, category, amount, addedBy) {
    // Just save the expense
    expenseDao.insert(expense);
    
    // Grocery affects meal rate calculation (month-end)
    // Others split equally (month-end)
}
```

**NEW Behavior:**
```java
void addExpense(messId, category, amount, addedBy, expenseDate) {
    // 1. Save the expense
    Expense expense = new Expense();
    expense.messId = messId;
    expense.category = category;
    expense.amount = amount;
    expense.addedBy = addedBy;
    expense.expenseDate = expenseDate;
    expenseDao.insert(expense);
    
    // 2. Update MessMonthlyStats for reporting
    updateMessMonthlyStats(messId, getMonth(expenseDate), getYear(expenseDate));
    
    // 3. Update MonthlyBills for all active members
    // (Bills are recalculated from Expenses table on-the-fly)
    List<User> activeMembers = userDao.getActiveMembersByDate(messId, expenseDate);
    for (User member : activeMembers) {
        updateMonthlyBill(member.userId, getMonth(expenseDate), getYear(expenseDate));
    }
}
```

**Key Points:**
- ALL expense categories now work the same way
- Automatic distribution to all active members
- Only charge members who joined before the expense date
- Real-time updates to personal expenses and monthly bills

---

### 3. **Daily Meal Auto-Charging** - NEW FEATURE

**File:** `MealAutoChargeService.java` (CREATE NEW)

```java
class MealAutoChargeService {
    
    // Run this daily (or when user opens app)
    void autoChargeMeals(userId) {
        // 1. Check if today's meal already exists
        Meal existingMeal = mealDao.getMealByDate(userId, today);
        if (existingMeal != null) return; // Already charged
        
        // 2. Get latest meal preference
        MealPreference preference = preferenceDao.getLatestPreference(userId);
        if (preference == null) return; // No preference set
        
        // 3. Check if preference is effective for today
        if (preference.effectiveFrom > today) return;
        
        // 4. Get current meal rate
        double mealRate = messDao.getMealRate(messId);
        
        // 5. Create today's meal record
        Meal todayMeal = new Meal();
        todayMeal.userId = userId;
        todayMeal.mealDate = today;
        todayMeal.breakfast = preference.breakfast;
        todayMeal.lunch = preference.lunch;
        todayMeal.dinner = preference.dinner;
        todayMeal.mealRate = mealRate;
        // mealExpense auto-calculated by database
        mealDao.insert(todayMeal);
        
        // 6. Update monthly bill (recalculates from Meals table)
        updateMonthlyBill(userId, getCurrentMonth(), getCurrentYear());
    }
}
```

**Trigger Points:**
- App startup (check and charge if needed)
- Background job (daily at midnight)
- When user opens meal section

---

### 4. **Monthly Bill Calculation** - Simplified

**File:** `MonthlyBillService.java`

**NEW Logic:**
```java
void updateMonthlyBill(userId, month, year) {
    // 1. Get or create monthly bill
    MonthlyBill bill = billDao.getBill(userId, month, year);
    if (bill == null) {
        bill = new MonthlyBill(userId, messId, month, year);
    }
    
    // 2. Calculate total meal expenses from Meals table
    double totalMealExpense = mealDao.getTotalMealExpense(userId, month, year);
    
    // 3. Calculate user's share of all other expenses
    // Get total expenses for the mess this month
    double totalMessExpenses = expenseDao.getTotalExpenses(messId, month, year);
    
    // Get member count (members active during this month)
    int memberCount = userDao.getActiveMemberCount(messId, month, year);
    
    // Calculate user's share
    double totalOtherExpenses = (memberCount > 0) ? (totalMessExpenses / memberCount) : 0;
    
    // 4. Update bill
    bill.totalMealExpense = totalMealExpense;
    bill.totalOtherExpenses = totalOtherExpenses;
    // totalBill and dueAmount auto-calculated by database
    
    // 5. Update status
    if (bill.totalPaid >= bill.totalBill) {
        bill.status = "paid";
    } else if (bill.totalPaid > 0) {
        bill.status = "partial";
    } else {
        bill.status = "pending";
    }
    
    billDao.update(bill);
}
```

---

### 5. **User DAO Update** - New Query Needed

**File:** `UserDao.java`

**Add New Methods:**
```java
@Query("SELECT * FROM Users WHERE messId = :messId AND isActive = 1 AND joinedDate <= :date")
List<User> getActiveMembersByDate(int messId, Date date);

@Query("SELECT COUNT(*) FROM Users WHERE messId = :messId AND isActive = 1 " +
       "AND strftime('%m', joinedDate) <= :month AND strftime('%Y', joinedDate) <= :year")
int getActiveMemberCount(int messId, int month, int year);
```

**Purpose:** 
- Get members who should be charged for an expense based on join date
- Count active members for a specific month/year

---

### 6. **MealDao Update** - New Query Needed

**File:** `MealDao.java`

**Add New Method:**
```java
@Query("SELECT SUM(mealExpense) FROM Meals WHERE userId = :userId " +
       "AND strftime('%m', mealDate) = :month AND strftime('%Y', mealDate) = :year")
double getTotalMealExpense(int userId, String month, String year);
```

**Purpose:** Calculate total meal expenses for a user in a specific month

---

### 7. **ExpenseDao Update** - New Query Needed

**File:** `ExpenseDao.java`

**Add New Method:**
```java
@Query("SELECT SUM(amount) FROM Expenses WHERE messId = :messId " +
       "AND strftime('%m', expenseDate) = :month AND strftime('%Y', expenseDate) = :year")
double getTotalExpenses(int messId, String month, String year);
```

**Purpose:** Get total expenses for a mess in a specific month (to be divided among members)

---

## UI Changes

### 1. **Dashboard Fragment** - Update Display

**File:** `fragment_dashboard.xml`
**File:** `fragment_meal.xml` 

**Changes: in dashboard**
- Show "Total Expense" (meal + shared)
- Total Meal count
- Real-time updates (no month-end calculation)

**Changes: in meal**
- Show "Total Meal Expense" (meal expenses)


---

### 2. **Meal Preference Dialog** - Update Behavior

**File:** `MealPreferenceDialog.java` or similar

**Changes:**
- Show confirmation with intelligent message based on today's meal status
- If already charged today: "Preference will apply from tomorrow"
- If not charged today: "You will be charged X TK for today"

**Code:**
```java
void onSavePreference() {
    int breakfast = getBreakfast();
    int lunch = getLunch();
    int dinner = getDinner();
    int totalMeals = breakfast + lunch + dinner;
    double mealRate = getMealRate();
    double todayCharge = totalMeals * mealRate;
    
    // Check if today already charged
    Meal existingMeal = mealDao.getMealByDate(userId, today);
    
    String message;
    if (existingMeal != null) {
        // Already charged today
        message = "Your meal preference will be updated.\n\n" +
                 "Today's meal is already recorded.\n" +
                 "New preference (" + totalMeals + " meals) will apply from tomorrow onwards.";
    } else {
        // Not charged yet
        message = "This will charge you ৳" + todayCharge + 
                 " for today (" + totalMeals + " meals @ ৳" + mealRate + " each).\n\n" +
                 "From tomorrow, this preference will continue automatically.";
    }
    
    // Show confirmation
    new AlertDialog.Builder(context)
        .setTitle("Confirm Meal Preference")
        .setMessage(message)
        .setPositiveButton("Confirm", (dialog, which) -> {
            mealPreferenceService.setMealPreference(userId, breakfast, lunch, dinner);
            
            String successMsg = (existingMeal != null) 
                ? "Preference updated! Will apply from tomorrow."
                : "Charged ৳" + todayCharge + " for today";
            Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show();
        })
        .setNegativeButton("Cancel", null)
        .show();
}
```

---

### 3. **Add Expense Activity** - Update Flow

**File:** `AddExpenseActivity.java`

**Changes:**
- Show message: "This expense will be split among X members (৳Y each)"
- Auto-calculate and display share amount

**Code:**
```java
void onAmountChanged(double amount) {
    int memberCount = userDao.getActiveMemberCount(messId);
    double sharePerMember = amount / memberCount;
    
    tvShareInfo.setText("Will be split among " + memberCount + 
                       " members (৳" + String.format("%.2f", sharePerMember) + " each)");
}

void onSaveExpense() {
    // Call the updated expense service
    expenseService.addExpense(messId, category, amount, userId, expenseDate);
    
    Toast.makeText(this, "Expense added and distributed to all members", Toast.LENGTH_SHORT).show();
    finish();
}
```

---

### 4. **Admin Dashboard** - Remove Month-End Features

**File:** `AdminDashboardFragment.java`

**Remove:**
- "Finalize Month" button
- "Calculate Final Rate" section
- Month-end adjustment UI

**Keep/Update:**
- Member list with real-time balances
- Total expense by category
- Current fixed meal rate display
- Payment recording

---

## Testing Checklist

### Unit Tests

- [ ] `MealDao.getTotalMealExpense()` - Calculates meal total correctly
- [ ] `ExpenseDao.getTotalExpenses()` - Calculates expense total correctly
- [ ] `UserDao.getActiveMemberCount()` - Returns correct count
- [ ] `MealPreferenceService.setMealPreference()` - Creates meal record for today
- [ ] `ExpenseService.addExpense()` - Saves expense and triggers bill updates
- [ ] `MonthlyBillService.updateMonthlyBill()` - Calculates shares correctly
- [ ] `MealAutoChargeService.autoChargeMeals()` - Daily charging works

### Integration Tests

- [ ] New user joins → Meal preference set → Charged immediately
- [ ] Expense added → All members get share → Bills updated
- [ ] Mid-month join → Only future expenses charged
- [ ] Meal preference changed → Old preference stops, new one starts
- [ ] Payment recorded → Due amount updates correctly

### UI Tests

- [ ] Dashboard shows correct totals
- [ ] Meal preference dialog shows charge confirmation
- [ ] Add expense shows share calculation
- [ ] Member list shows real-time balances

---

## Migration Steps

### Step 1: Database Migration
1. Create migration script with all ALTER TABLE statements
2. Create PersonalExpenses table
3. Update Meals table with new fields
4. Simplify MonthlyBills and MessMonthlyStats tables
5. Test migration on sample database

### Step 2: Update Existing DAOs
1. `MealDao.java` - Add `getTotalMealExpense()` query
2. `ExpenseDao.java` - Add `getTotalExpenses()` query
3. `UserDao.java` - Add `getActiveMembersByDate()` and `getActiveMemberCount()`
4. Create `MealAutoChargeService.java` for daily charging

### Step 3: Update Existing Code
1. Update `MealPreferenceService` - Add immediate charging
2. Update `ExpenseService` - Add distribution logic
3. Update `MonthlyBillService` - Simplify calculation
4. Update `UserDao` - Add getActiveMembersByDate()

### Step 4: UI Updates
1. Update Dashboard layout and logic
2. Update Meal Preference dialog with confirmation
3. Update Add Expense with share preview
4. Remove month-end features from Admin dashboard

### Step 5: Testing
1. Unit tests for all new/updated services
2. Integration tests for complete flows
3. Manual testing of all user scenarios
4. Performance testing with multiple users

### Step 6: Deployment
1. Backup current database
2. Run migration script
3. Deploy updated APK
4. Monitor for issues

---

## Estimated Effort

| Task | Effort |
|------|--------|
| Database schema changes | 2 hours |
| Update DAO queries (Meal/Expense/User) | 2 hours |
| Update MealPreferenceService | 2 hours |
| Update ExpenseService | 2 hours |
| MealAutoChargeService | 2 hours |
| Update MonthlyBillService | 3 hours |
| UI updates | 4 hours |
| Testing | 3 hours |
| **Total** | **20 hours** |

---

## Priority Order

1. **HIGH PRIORITY** (Core Functionality)
   - Database schema changes (Meals table)
   - DAO query updates (Meal/Expense/User)
   - MonthlyBill calculation with on-the-fly sharing
   - Meal preference immediate charging

2. **MEDIUM PRIORITY** (User Experience)
   - Dashboard UI updates
   - Meal preference confirmation dialog
   - Daily auto-charging service

3. **LOW PRIORITY** (Nice to Have)
   - Advanced reporting
   - Expense breakdown charts
   - Historical data cleanup

---

## Rollback Plan

If issues occur:
1. Restore database backup
2. Revert to previous APK version
3. Keep old calculation logic as fallback
4. Gradual rollout to test users first

---

## Notes

- All monetary calculations should use `BigDecimal` to avoid floating-point errors
- Add proper error handling for division by zero (when memberCount = 0)
- Implement transaction management for multi-step operations (expense distribution)
- Add logging for all automatic charges (audit trail)
- Consider adding settings to disable auto-charging if needed
