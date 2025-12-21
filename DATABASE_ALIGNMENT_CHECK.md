# Database Schema vs DAO/Model Alignment Check
**Date:** December 22, 2025  
**Status:** ✅ **ALL ALIGNED**

---

## Summary
All model classes and DAO classes are properly aligned with the database schema. No mismatches found.

---

## Table-by-Table Verification

### 1. **Users Table** ✅

**Database Schema:**
```sql
userId INTEGER PRIMARY KEY AUTOINCREMENT
fullName TEXT NOT NULL
email TEXT UNIQUE NOT NULL
phoneNumber TEXT UNIQUE NOT NULL
password TEXT NOT NULL
messId INTEGER
role TEXT DEFAULT 'member'
joinedDate INTEGER DEFAULT (strftime('%s', 'now'))
isActive INTEGER DEFAULT 1
FOREIGN KEY (messId) REFERENCES Mess(messId)
```

**Model (User.java):** ✅ ALIGNED
- ✅ `long userId` - Maps to INTEGER PRIMARY KEY
- ✅ `String fullName` - Maps to TEXT NOT NULL
- ✅ `String email` - Maps to TEXT UNIQUE NOT NULL
- ✅ `String phoneNumber` - Maps to TEXT UNIQUE NOT NULL
- ✅ `int messId` - Maps to INTEGER (nullable)
- ✅ `String role` - Maps to TEXT DEFAULT 'member'
- ✅ `long joinedDate` - Maps to INTEGER (Unix timestamp)
- ⚠️ `password` - Not in model (intentionally omitted for security)
- ⚠️ `isActive` - Not in model (handled by DAO queries)

**DAO (UserDao.java):** ✅ ALIGNED
- ✅ `registerUser()` - Inserts all required fields
- ✅ `loginUser()` - Checks email, password, isActive
- ✅ `getUserByIdAsObject()` - Returns User object with all fields
- ✅ `getMembersByMessId()` - Filters by messId and isActive
- ✅ `updateUserMessId()` - Updates messId field
- ✅ `updateUserRole()` - Updates role field
- ✅ `deleteUser()` - Soft delete (sets isActive = 0)

**Note:** Password and isActive intentionally not in model class for security and architectural reasons.

---

### 2. **Mess Table** ✅

**Database Schema:**
```sql
messId INTEGER PRIMARY KEY AUTOINCREMENT
messName TEXT NOT NULL
groceryBudgetPerMeal REAL NOT NULL DEFAULT 40.00
cookingChargePerMeal REAL NOT NULL DEFAULT 10.00
createdDate INTEGER DEFAULT (strftime('%s','now'))
```

**Model (Mess.java):** ✅ PERFECTLY ALIGNED
- ✅ `int messId` - Maps to INTEGER PRIMARY KEY
- ✅ `String messName` - Maps to TEXT NOT NULL
- ✅ `double groceryBudgetPerMeal` - Maps to REAL NOT NULL
- ✅ `double cookingChargePerMeal` - Maps to REAL NOT NULL
- ✅ `long createdDate` - Maps to INTEGER (Unix timestamp)
- ✅ Helper: `getInvitationCode()` - Calculates messId + 999

**DAO (MessDao.java):** ✅ ALIGNED
- ✅ `createMess()` - Inserts all required fields
- ✅ `getMessByIdAsObject()` - Returns complete Mess object
- ✅ `updateMessRates()` - Updates grocery and cooking charge
- ✅ `getMessNameById()` - Retrieves just messName
- ✅ `updateMessName()` - Updates messName field

---

### 3. **Expenses Table** ✅

**Database Schema:**
```sql
expenseId INTEGER PRIMARY KEY AUTOINCREMENT
messId INTEGER NOT NULL
addedBy INTEGER NOT NULL
category TEXT NOT NULL
amount REAL NOT NULL
description TEXT
expenseDate INTEGER NOT NULL
createdAt INTEGER DEFAULT (strftime('%s', 'now'))
updatedAt INTEGER DEFAULT (strftime('%s', 'now'))
FOREIGN KEY (messId) REFERENCES Mess(messId)
FOREIGN KEY (addedBy) REFERENCES Users(userId)
```

**Model (Expense.java):** ✅ ALIGNED
- ✅ `int expenseId` - Maps to INTEGER PRIMARY KEY
- ✅ `int messId` - Maps to INTEGER NOT NULL
- ✅ `int addedBy` - Maps to INTEGER NOT NULL
- ✅ `String category` - Maps to TEXT NOT NULL
- ✅ `double amount` - Maps to REAL NOT NULL
- ✅ `String description` - Maps to TEXT (nullable)
- ✅ `long expenseDate` - Maps to INTEGER NOT NULL
- ✅ `long createdAt` - Maps to INTEGER
- ✅ `String addedByName` - Derived field (from JOIN with Users)
- ⚠️ `updatedAt` - Not in model (tracked in DB only)

**DAO (ExpenseDao.java):** ✅ ALIGNED
- ✅ `addExpense()` - Inserts all required fields + timestamps
- ✅ `getExpensesByMonth()` - JOINs with Users to get addedByName
- ✅ `getExpensesByCategory()` - Filters by category
- ✅ `getTotalExpenseByCategory()` - SUM aggregation
- ✅ `updateExpense()` - Updates amount, description, updatedAt
- ✅ `deleteExpense()` - Deletes by expenseId

---

### 4. **Meals Table** ✅

**Database Schema:**
```sql
mealId INTEGER PRIMARY KEY AUTOINCREMENT
userId INTEGER NOT NULL
messId INTEGER NOT NULL
mealDate INTEGER NOT NULL
breakfast INTEGER DEFAULT 1
lunch INTEGER DEFAULT 1
dinner INTEGER DEFAULT 1
createdAt INTEGER DEFAULT (strftime('%s', 'now'))
updatedAt INTEGER DEFAULT (strftime('%s', 'now'))
UNIQUE(userId, mealDate)
FOREIGN KEY (userId) REFERENCES Users(userId)
FOREIGN KEY (messId) REFERENCES Mess(messId)
```

**Model (Meal.java):** ✅ ALIGNED
- ✅ `int mealId` - Maps to INTEGER PRIMARY KEY
- ✅ `int userId` - Maps to INTEGER NOT NULL
- ✅ `int messId` - Maps to INTEGER NOT NULL
- ✅ `long mealDate` - Maps to INTEGER NOT NULL (Unix timestamp)
- ✅ `int breakfast` - Maps to INTEGER DEFAULT 1
- ✅ `int lunch` - Maps to INTEGER DEFAULT 1
- ✅ `int dinner` - Maps to INTEGER DEFAULT 1
- ✅ `int totalMeals` - Calculated field (breakfast + lunch + dinner)
- ⚠️ `createdAt/updatedAt` - Not in model (tracked in DB only)

**DAO (MealDao.java):** ✅ ALIGNED
- ✅ `addOrUpdateMeal()` - Uses CONFLICT_REPLACE for UNIQUE constraint
- ✅ `getMealByDate()` - Queries by userId and mealDate
- ✅ `getMealsByMonth()` - Range query on mealDate
- ✅ `getTotalMealsForMonth()` - SUM(breakfast + lunch + dinner)
- ✅ `getTotalMessMealsForMonth()` - Aggregates all members
- ✅ `deleteMeal()` - Deletes by userId and mealDate

---

### 5. **MealPreferences Table** ✅

**Database Schema:**
```sql
preferenceId INTEGER PRIMARY KEY AUTOINCREMENT
userId INTEGER NOT NULL
messId INTEGER NOT NULL
breakfast INTEGER DEFAULT 1
lunch INTEGER DEFAULT 1
dinner INTEGER DEFAULT 1
effectiveFrom INTEGER NOT NULL
createdAt INTEGER DEFAULT (strftime('%s', 'now'))
FOREIGN KEY (userId) REFERENCES Users(userId)
FOREIGN KEY (messId) REFERENCES Mess(messId)
```

**Model:** ❌ **NO MODEL CLASS**
**Reason:** MealPreferences is a configuration table, not a business entity. Data is accessed directly via DAO and applied to Meal entries.

**DAO (MealDao.java):** ✅ ALIGNED
- ✅ `saveMealPreference()` - Inserts all required fields
- ✅ `getMealPreference()` - Returns int[] {breakfast, lunch, dinner}
- ✅ Calculates `effectiveFrom` as tomorrow's date
- ✅ Uses ORDER BY createdAt DESC to get latest preference

**Decision:** No model class needed - preference data is applied to Meal model.

---

### 6. **MonthlyStats Table** ✅

**Database Schema:**
```sql
statsId INTEGER PRIMARY KEY AUTOINCREMENT
messId INTEGER NOT NULL
month INTEGER NOT NULL
year INTEGER NOT NULL
totalGrocery REAL DEFAULT 0.00
totalUtilities REAL DEFAULT 0.00
totalCleaning REAL DEFAULT 0.00
totalGas REAL DEFAULT 0.00
totalRent REAL DEFAULT 0.00
totalMiscellaneous REAL DEFAULT 0.00
totalMeals INTEGER DEFAULT 0
numberOfMembers INTEGER NOT NULL
cookingCharge REAL NOT NULL
isFinalized INTEGER DEFAULT 0
finalizedDate INTEGER
createdAt INTEGER DEFAULT (strftime('%s','now'))
UNIQUE(messId, month, year)
FOREIGN KEY (messId) REFERENCES Mess(messId)
```

**Model:** ❌ **NO MODEL CLASS**
**Reason:** Monthly statistics are calculated and aggregated by ReportDao. Not a user-facing entity.

**DAO (ReportDao.java):** ✅ ALIGNED
- ✅ `getTotalExpenses()` - Calculates from Expenses table
- ✅ `getGroceryTotal()` - Aggregates grocery category
- ✅ `getTotalMeals()` - Aggregates from Meals table
- ✅ `calculateMealRate()` - Formula: groceryTotal / totalMeals + cookingCharge
- ✅ `getExpenseBreakdown()` - Returns array of all 6 categories

**Note:** MonthlyStats table is for month-end finalization (future feature). Current implementation calculates on-the-fly.

---

### 7. **MonthlyBills Table** ✅

**Database Schema:**
```sql
billId INTEGER PRIMARY KEY AUTOINCREMENT
userId INTEGER NOT NULL
messId INTEGER NOT NULL
month INTEGER NOT NULL
year INTEGER NOT NULL
totalMeals INTEGER NOT NULL
mealRate REAL NOT NULL
utilitiesShare REAL DEFAULT 0.00
cleaningShare REAL DEFAULT 0.00
gasShare REAL DEFAULT 0.00
rentShare REAL DEFAULT 0.00
miscShare REAL DEFAULT 0.00
totalPaid REAL DEFAULT 0.00
status TEXT DEFAULT 'pending'
finalizedDate INTEGER
createdAt INTEGER DEFAULT (strftime('%s', 'now'))
updatedAt INTEGER DEFAULT (strftime('%s', 'now'))
UNIQUE(userId, messId, month, year)
FOREIGN KEY (userId) REFERENCES Users(userId)
FOREIGN KEY (messId) REFERENCES Mess(messId)
```

**Model (MemberBalance.java):** ✅ PARTIAL ALIGNMENT
**Purpose:** Display model for showing member bills (subset of MonthlyBills)

Fields in MemberBalance:
- ✅ `long userId` - Maps to userId
- ✅ `String fullName` - From Users table (JOIN)
- ✅ `int totalMeals` - Maps to totalMeals
- ✅ `double totalBill` - Calculated (mealCost + all shares)
- ✅ `double totalPaid` - Maps to totalPaid
- ✅ `double dueAmount` - Calculated (totalBill - totalPaid)
- ✅ `String paymentStatus` - Derived from status

**DAO (ReportDao.java):** ✅ ALIGNED
- ✅ `getMemberBalances()` - Calculates bills on-the-fly
- ✅ Uses MealDao to get totalMeals
- ✅ Calculates meal cost: totalMeals × mealRate
- ✅ Calculates shared expenses per member
- ✅ Gets totalPaid from Payments table

**Note:** Current implementation calculates bills dynamically. MonthlyBills table is for month-end finalization (future feature).

---

### 8. **Payments Table** ✅

**Database Schema:**
```sql
paymentId INTEGER PRIMARY KEY AUTOINCREMENT
billId INTEGER NOT NULL
userId INTEGER NOT NULL
messId INTEGER NOT NULL
amount REAL NOT NULL
paidDate INTEGER NOT NULL
addedBy INTEGER NOT NULL
paymentMethod TEXT DEFAULT 'Cash'
notes TEXT
createdAt INTEGER DEFAULT (strftime('%s', 'now'))
FOREIGN KEY (billId) REFERENCES MonthlyBills(billId)
FOREIGN KEY (userId) REFERENCES Users(userId)
FOREIGN KEY (messId) REFERENCES Mess(messId)
FOREIGN KEY (addedBy) REFERENCES Users(userId)
```

**Model:** ❌ **NO MODEL CLASS**
**Reason:** Payment tracking is admin functionality (future feature). Currently tracked in DB only.

**DAO (ReportDao.java):** ✅ ALIGNED
- ✅ `getTotalPaid()` - SUM(amount) filtered by userId, messId, month, year
- ✅ Queries Payments table for date range

**Note:** Payment model and full DAO will be added in Phase 6.6 (Admin features).

---

## Data Type Mappings ✅

All data types are correctly mapped:

| SQL Type | Java Type | Notes |
|----------|-----------|-------|
| INTEGER  | int / long | long for IDs, int for counts |
| REAL     | double | For currency and rates |
| TEXT     | String | For all text fields |
| INTEGER (timestamp) | long | Unix timestamp (seconds) |

---

## Foreign Key Handling ✅

All foreign keys are properly handled:

1. **Users.messId → Mess.messId**
   - DAO: UserDao uses messId correctly
   - Model: User.messId matches

2. **Expenses.messId → Mess.messId**
   - DAO: ExpenseDao enforces messId
   - Model: Expense.messId matches

3. **Expenses.addedBy → Users.userId**
   - DAO: ExpenseDao JOINs to get addedByName
   - Model: Expense has both addedBy and addedByName

4. **Meals.userId → Users.userId**
   - DAO: MealDao filters by userId
   - Model: Meal.userId matches

5. **Meals.messId → Mess.messId**
   - DAO: MealDao uses messId for aggregation
   - Model: Meal.messId matches

---

## Unique Constraints ✅

All unique constraints are respected:

1. **Users: UNIQUE(email)**
   - DAO: `isEmailExists()` checks before insert

2. **Users: UNIQUE(phoneNumber)**
   - DAO: `isPhoneExists()` checks before insert

3. **Meals: UNIQUE(userId, mealDate)**
   - DAO: Uses `CONFLICT_REPLACE` strategy

4. **MonthlyStats: UNIQUE(messId, month, year)**
   - DAO: Not yet implemented (future feature)

5. **MonthlyBills: UNIQUE(userId, messId, month, year)**
   - DAO: Not yet implemented (future feature)

---

## Missing Model Classes (Intentional)

### 1. **MealPreferences** - No model needed
- Preferences are applied to Meal entries
- DAO returns int[] array directly

### 2. **MonthlyStats** - Future feature
- Month-end finalization not yet implemented
- ReportDao calculates statistics on-the-fly

### 3. **MonthlyBills** - Partially implemented
- MemberBalance model serves as display model
- Full billing system is future feature

### 4. **Payments** - Future feature
- Admin payment tracking not yet implemented
- ReportDao queries directly for totals

---

## Recommendations

### ✅ **No Changes Needed** for Phase 6.2

All models and DAOs are properly aligned for implementing the 5 fragments:
- DashboardFragment
- MealFragment
- ExpenseFragment
- ReportFragment
- SettingsFragment

### 📋 **Future Enhancements** (Post Phase 6.5)

1. **Payment Model & PaymentDao**
   - For admin payment tracking
   - Full payment history

2. **MonthlyStats finalization**
   - Month-end closing functionality
   - Historical reporting

3. **MonthlyBills generation**
   - Automated bill creation
   - Bill notifications

---

## Conclusion ✅

**All model classes and DAO classes are properly aligned with the database schema.**

### Alignment Summary:
- ✅ User.java ↔️ Users table - **100% aligned**
- ✅ Mess.java ↔️ Mess table - **100% aligned**
- ✅ Expense.java ↔️ Expenses table - **100% aligned**
- ✅ Meal.java ↔️ Meals table - **100% aligned**
- ✅ MemberBalance.java ↔️ MonthlyBills table - **Partial (by design)**

### DAO Coverage:
- ✅ UserDao.java - **Complete**
- ✅ MessDao.java - **Complete**
- ✅ MealDao.java - **Complete**
- ✅ ExpenseDao.java - **Complete**
- ✅ ReportDao.java - **Complete for current phase**

**Status:** Ready to proceed with Phase 6.2 (Fragment Creation) ✅
