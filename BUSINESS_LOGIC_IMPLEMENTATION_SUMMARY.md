# Business Logic Implementation Summary - Phase 6.3

## Overview
This document summarizes all business logic changes implemented to support the fixed meal rate system with immediate charging.

## Implementation Date
Current implementation phase

---

## 1. MealFragment.java Updates

### Changes Made
1. **Added MessDao Import and Instance**
   - Added `import com.messkhata.data.dao.MessDao` and `import com.messkhata.data.model.Mess`
   - Added `private MessDao messDao;` field
   - Initialized in `initializeDAO()` method

2. **Updated saveTodayMeal() Method**
   - Now retrieves mess information to get current meal rate
   - Calculates `mealRate = groceryBudgetPerMeal + cookingChargePerMeal`
   - Passes `mealRate` parameter to `mealDao.addOrUpdateMeal()`
   - Provides error handling if mess not found

### Code Flow
```java
saveTodayMeal() {
    1. Get Mess object from MessDao
    2. Calculate mealRate (grocery + cooking)
    3. Call addOrUpdateMeal() with mealRate
    4. Handle errors appropriately
}
```

### Impact
- Meals are now automatically charged at the current rate when saved
- No manual calculation needed - rate is fetched from Mess table
- Ensures consistency with mess configuration

---

## 2. ExpenseDao.java Updates

### Changes Made
1. **Added getTotalExpenses() Method**
   - Calculates total expenses for a mess in a specific month
   - Used for expense distribution calculations
   - Returns sum of all expense amounts

### Method Signature
```java
public double getTotalExpenses(int messId, int month, int year)
```

### SQL Query
```sql
SELECT SUM(amount) as total 
FROM Expenses 
WHERE messId = ? AND expenseDate >= ? AND expenseDate < ?
```

### Impact
- Enables calculation of shared expenses among active members
- Foundation for bill calculation: (totalExpenses / activeMembers)
- Can be called from reporting and bill generation logic

---

## 3. UserDao.java Updates

### Changes Made
1. **Added Calendar Import**
   - Added `import java.util.Calendar;` for date calculations

2. **Added getActiveMembersByDate() Method**
   - Returns list of user IDs who had meals on a specific date
   - Used for daily expense distribution

3. **Added getActiveMemberCount() Method**
   - Returns count of distinct members who had meals in a month
   - Used for monthly bill calculations

### Method Signatures
```java
public List<Integer> getActiveMembersByDate(int messId, long date)
public int getActiveMemberCount(int messId, int month, int year)
```

### SQL Queries
**getActiveMembersByDate:**
```sql
SELECT DISTINCT userId 
FROM Meals 
WHERE messId = ? AND mealDate = ? 
  AND (breakfast > 0 OR lunch > 0 OR dinner > 0)
```

**getActiveMemberCount:**
```sql
SELECT COUNT(DISTINCT userId) as count 
FROM Meals 
WHERE messId = ? AND mealDate >= ? AND mealDate < ? 
  AND (breakfast > 0 OR lunch > 0 OR dinner > 0)
```

### Impact
- Enables accurate expense distribution among only active members
- Inactive members (no meals) are not charged for shared expenses
- Supports both daily and monthly calculations

---

## 4. MealDao.java Updates

### Changes Made
1. **Added getActivePreferences() Method**
   - Returns cursor with latest meal preferences for all users in a mess
   - Uses subquery to get only the most recent preference per user
   - Used by MealAutoChargeService for daily auto-charging

### Method Signature
```java
public Cursor getActivePreferences(int messId)
```

### SQL Query
```sql
SELECT mp.userId, mp.breakfast, mp.lunch, mp.dinner 
FROM MealPreferences mp 
INNER JOIN (
    SELECT userId, MAX(createdAt) as maxCreated 
    FROM MealPreferences 
    WHERE messId = ? 
    GROUP BY userId
) latest ON mp.userId = latest.userId AND mp.createdAt = latest.maxCreated 
WHERE mp.messId = ?
```

### Impact
- Enables batch processing of meal preferences
- Ensures only latest preference is used per user
- Foundation for automated daily charging system

---

## 5. New Service: MealAutoChargeService.java

### Purpose
Automated service for charging users daily based on their saved meal preferences.

### Location
`/app/src/main/java/com/messkhata/service/MealAutoChargeService.java`

### Key Methods

#### 1. processAutoCharging(int messId)
- **Purpose:** Process automatic charging for all users with preferences
- **When to Call:** Daily, preferably early morning (via WorkManager/AlarmManager)
- **Returns:** Number of users successfully charged
- **Logic:**
  1. Get all active preferences for the mess
  2. For each user with preference:
     - Check if at least one meal selected
     - Call chargeTodayMeal() if not already charged
  3. Return count of successful charges

#### 2. chargeTodayMeal(userId, messId, breakfast, lunch, dinner)
- **Purpose:** Charge a specific user for today's meals
- **Returns:** true if charged, false if already charged or error
- **Logic:**
  1. Check if already charged for today (getMealByDate)
  2. If already charged, return false
  3. Get mess rate from Mess table
  4. Add meal entry with preference values
  5. Return success status

#### 3. getTodayTimestamp()
- **Purpose:** Get today's date at midnight (00:00:00)
- **Returns:** Timestamp in seconds
- **Note:** Ensures consistent date comparison across system

### Integration Points
```java
// Example usage in a daily job:
MealAutoChargeService service = new MealAutoChargeService(context);
int chargedCount = service.processAutoCharging(messId);
Log.d("AutoCharge", "Charged " + chargedCount + " users");
```

### Future Enhancements
- Integrate with WorkManager for scheduled daily execution
- Add notification system for auto-charged meals
- Add admin dashboard to view auto-charge statistics
- Add user settings to enable/disable auto-charging

---

## 6. Business Logic Flow

### Manual Meal Entry Flow
```
User adjusts meal counters in MealFragment
    ↓
updateTotalAndSave() called
    ↓
saveTodayMeal() executes:
    1. Get Mess object (with rates)
    2. Calculate mealRate
    3. Save to Meals table with rate
    ↓
Meal expense automatically calculated (meals × mealRate)
```

### Automatic Daily Charging Flow
```
Daily scheduler triggers (WorkManager/AlarmManager)
    ↓
MealAutoChargeService.processAutoCharging(messId)
    ↓
For each user with preference:
    1. Check if already charged today
    2. If not charged:
        a. Get mess rate
        b. Create meal entry with preference
        c. Charge automatically
    ↓
Users see meals in their daily view
```

### Expense Distribution Flow
```
Admin adds expense
    ↓
System calculates distribution:
    1. Get total expenses (ExpenseDao.getTotalExpenses)
    2. Get active member count (UserDao.getActiveMemberCount)
    3. Calculate share = totalExpenses / activeMemberCount
    ↓
Each member's bill = mealExpense + expenseShare
```

### Monthly Bill Calculation Flow
```
End of month / Bill generation request
    ↓
For each user:
    1. Get total meal expense (MealDao.getTotalMealExpense)
    2. Calculate shared expense portion:
        a. Get total expenses for month
        b. Get active member count
        c. Calculate share = total / count
    3. Total bill = mealExpense + sharedExpense
    ↓
Store in MonthlyBills table (simplified schema)
```

---

## 7. Database Interaction Summary

### Tables Used
1. **Mess** - Source of meal rates (groceryBudgetPerMeal, cookingChargePerMeal)
2. **Meals** - Stores daily meal counts with mealRate (for charging)
3. **MealPreferences** - Stores user preferences for auto-charging
4. **Expenses** - All mess expenses (distributed among active members)
5. **Users** - Member information and mess association

### Key Calculations
1. **Meal Rate:** `groceryBudgetPerMeal + cookingChargePerMeal`
2. **Meal Expense:** `(breakfast + lunch + dinner) × mealRate`
3. **Total Meal Expense:** `SUM((breakfast + lunch + dinner) × mealRate)` for month
4. **Shared Expense:** `totalExpenses / activeMemberCount`
5. **Monthly Bill:** `totalMealExpense + sharedExpense`

---

## 8. Testing Checklist

### Unit Testing
- [ ] MealFragment.saveTodayMeal() with valid mess
- [ ] MealFragment.saveTodayMeal() with invalid mess
- [ ] ExpenseDao.getTotalExpenses() for empty month
- [ ] ExpenseDao.getTotalExpenses() with multiple expenses
- [ ] UserDao.getActiveMembersByDate() with no meals
- [ ] UserDao.getActiveMembersByDate() with multiple members
- [ ] UserDao.getActiveMemberCount() for entire month
- [ ] MealDao.getActivePreferences() with no preferences
- [ ] MealDao.getActivePreferences() with multiple users
- [ ] MealAutoChargeService.chargeTodayMeal() first time
- [ ] MealAutoChargeService.chargeTodayMeal() duplicate prevention
- [ ] MealAutoChargeService.processAutoCharging() batch processing

### Integration Testing
- [ ] Manual meal entry → Check Meals table has mealRate
- [ ] Auto-charge → Verify meal entry created with preference
- [ ] Auto-charge → Verify no duplicate entries
- [ ] Expense addition → Verify distribution calculation
- [ ] Monthly bill → Verify correct totals (meals + shared)
- [ ] Rate change → Verify new meals use new rate
- [ ] Inactive member → Verify not counted in distribution

### End-to-End Testing
- [ ] Full month cycle: preferences → auto-charge → expenses → bills
- [ ] Multiple members with different preferences
- [ ] Member joins mid-month → only charged from join date
- [ ] Member deactivated → not charged after deactivation
- [ ] Rate changed mid-month → meals use rate at time of entry

---

## 9. Migration Notes

### From Previous System
- Old system: Variable meal rates, month-end calculation
- New system: Fixed rates, immediate charging
- **No data migration needed** - database schema already updated
- PersonalExpenses table removed - calculations now on-the-fly

### Backward Compatibility
- Existing Meals entries: Compatible (mealRate field added)
- Existing code: Updated to pass mealRate parameter
- Old preferences: Will work with new auto-charge system

---

## 10. Future Enhancements

### Planned Features
1. **WorkManager Integration**
   - Schedule MealAutoChargeService daily
   - Configurable execution time (e.g., 2:00 AM)
   - Retry on failure

2. **Notification System**
   - Notify users when auto-charged
   - Show meal expense in notification
   - Option to edit meal within grace period

3. **Admin Dashboard**
   - View auto-charge statistics
   - See how many users charged daily
   - Monitor service health

4. **User Settings**
   - Enable/disable auto-charging
   - Set auto-charge time preference
   - Grace period for meal editing

5. **Expense Categories**
   - Better categorization of shared expenses
   - Option for non-shared expenses
   - Category-wise distribution rules

---

## Summary

All business logic changes have been successfully implemented:

✅ **MealFragment** - Updated to pass mealRate when saving meals
✅ **ExpenseDao** - Added getTotalExpenses() for expense distribution
✅ **UserDao** - Added methods for active member tracking
✅ **MealDao** - Added getActivePreferences() for auto-charging
✅ **MealAutoChargeService** - New service for automated daily charging

### Key Achievements
- Immediate meal charging with fixed rates
- Automated preference-based charging system
- Equal expense distribution among active members only
- On-the-fly calculation (no PersonalExpenses table)
- Clean separation of concerns (DAO, Fragment, Service)

### No Compilation Errors
All code compiles successfully with no errors.

---

**Implementation Status:** ✅ COMPLETE
**Last Updated:** Current Session
**Next Steps:** Integration testing and WorkManager setup
