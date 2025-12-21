# MessKhata - Phase 6 Implementation Plan
## Complete Dashboard & Fragment Functionality

---

## Current State Analysis

### ✅ **Completed (Phases 1-5)**
- Database schema (8 tables, version 2)
- User authentication (signup, login, session management)
- Mess setup (create/join with invitation code)
- MainActivity session validation & prefManager initialization
- XML layout consistency fixes

### ❌ **Missing Critical Components**

#### 1. **Fragment Classes (0/5 created)**
- No `DashboardFragment.java`
- No `MealFragment.java`
- No `ExpenseFragment.java`
- No `ReportFragment.java`
- No `SettingsFragment.java`

#### 2. **MainActivity Integration**
- No fragment initialization in `onCreate()`
- No bottom navigation listener
- No fragment transaction logic
- Empty `setupBottomNavigation()` method

#### 3. **Data Access Layer (Incomplete)**
- ✅ `UserDao.java` - Basic operations only (register, login, check email/phone)
- ✅ `MessDao.java` - Basic operations only (create, join)
- ❌ Missing: MealDao, ExpenseDao, ReportDao
- ❌ Missing: User profile queries, mess info queries

#### 4. **Model Classes (0 created)**
- No User model
- No Mess model
- No Expense model
- No Meal model
- No Member model (for displaying member list)

---

## Implementation Phases

### **Phase 6.1: Core Infrastructure** ⭐ (CRITICAL)

#### Task 1: Create Model Classes
**Purpose:** Data structures for passing information between DAO and UI

**Files to Create:**
1. `/data/model/User.java`
   ```java
   - userId, fullName, email, phoneNumber, messId, role, joinedDate
   ```

2. `/data/model/Mess.java`
   ```java
   - messId, messName, groceryBudgetPerMeal, cookingChargePerMeal, invitationCode
   ```

3. `/data/model/Expense.java`
   ```java
   - expenseId, messId, addedBy, category, amount, description, expenseDate, addedByName
   ```

4. `/data/model/Meal.java`
   ```java
   - mealId, userId, messId, mealDate, breakfast, lunch, dinner, totalMeals
   ```

5. `/data/model/MemberBalance.java`
   ```java
   - userId, fullName, totalMeals, totalBill, totalPaid, dueAmount
   ```

---

#### Task 2: Extend DAO Classes
**Purpose:** Add queries needed by fragments

**Files to Modify:**

**A. `/data/dao/UserDao.java`** - Add methods:
- `User getUserById(long userId)` - Get full user details
- `User getUserWithMess(long userId)` - Get user with mess info joined
- `List<User> getMembersByMessId(int messId)` - Get all mess members
- `boolean updateUserMessId(long userId, int messId)` - Update user's mess
- `boolean updateUserRole(long userId, String role)` - Admin can change roles

**B. `/data/dao/MessDao.java`** - Add methods:
- `Mess getMessById(int messId)` - Get full mess details
- `boolean updateMessRates(int messId, double groceryBudget, double cookingCharge)` - Admin updates rates
- `String getMessNameById(int messId)` - Get mess name only

**C. `/data/dao/MealDao.java`** (NEW FILE) - Create with:
- `boolean addOrUpdateMeal(int userId, int messId, long date, int breakfast, int lunch, int dinner)` - Save meal entry
- `Meal getMealByDate(int userId, long date)` - Get user's meal for specific date
- `List<Meal> getMealsByMonth(int userId, int month, int year)` - Monthly view
- `int getTotalMealsForMonth(int userId, int month, int year)` - Count user's meals
- `int getTotalMessMealsForMonth(int messId, int month, int year)` - Count all members' meals
- `boolean saveMealPreference(int userId, int messId, int breakfast, int lunch, int dinner)` - Save default preference
- `int[] getMealPreference(int userId)` - Get user's current preference [breakfast, lunch, dinner]

**D. `/data/dao/ExpenseDao.java`** (NEW FILE) - Create with:
- `long addExpense(int messId, int addedBy, String category, double amount, String description, long expenseDate)` - Add expense
- `List<Expense> getExpensesByMonth(int messId, int month, int year)` - Monthly expenses
- `List<Expense> getExpensesByCategory(int messId, String category, int month, int year)` - Filter by category
- `double getTotalExpenseByCategory(int messId, String category, int month, int year)` - Sum expenses
- `boolean updateExpense(int expenseId, double amount, String description)` - Edit expense
- `boolean deleteExpense(int expenseId)` - Delete expense (admin only)

**E. `/data/dao/ReportDao.java`** (NEW FILE) - Create with:
- `List<MemberBalance> getMemberBalances(int messId, int month, int year)` - All member bills
- `double getTotalExpenses(int messId, int month, int year)` - Sum all expenses
- `double getGroceryTotal(int messId, int month, int year)` - Grocery expenses only
- `int getTotalMeals(int messId, int month, int year)` - All meals consumed
- `double calculateMealRate(int messId, int month, int year)` - groceryTotal / totalMeals + cookingCharge

---

### **Phase 6.2: Fragment Creation**

#### Task 3: Create DashboardFragment
**File:** `/ui/fragment/DashboardFragment.java`

**Features:**
- Display welcome message with user name
- Show mess information (mess name, invitation code)
- Display current month summary:
  - Total meals taken
  - Estimated bill (meals × fixed rate)
  - Amount paid
  - Due amount
- Quick stats cards:
  - Total mess expenses
  - Total members
  - Meal rate
- Swipe to refresh functionality
- Sync status indicator

**Dependencies:**
- `UserDao.getUserById()`
- `MessDao.getMessById()`
- `MealDao.getTotalMealsForMonth()`
- `ReportDao.getTotalExpenses()`

---

#### Task 4: Create MealFragment
**File:** `/ui/fragment/MealFragment.java`

**Features:**
- Calendar view showing current month
- Meal entry for today:
  - Breakfast counter (+ / - buttons)
  - Lunch counter (+ / - buttons)
  - Dinner counter (+ / - buttons)
  - Save button
- Meal preference section:
  - Set default breakfast/lunch/dinner counts
  - "Apply from Tomorrow" button
- Monthly meal summary:
  - Total meals this month
  - Breakdown by meal type
- Date navigation (previous/next day)

**Dependencies:**
- `MealDao.getMealByDate()`
- `MealDao.addOrUpdateMeal()`
- `MealDao.saveMealPreference()`
- `MealDao.getMealPreference()`
- `MealDao.getTotalMealsForMonth()`

**UI Components:**
- Replace meal preference switches with counter layout:
  ```xml
  <LinearLayout> (for each meal type)
      <TextView>Breakfast</TextView>
      <Button>-</Button>
      <TextView>1</TextView> (count)
      <Button>+</Button>
  </LinearLayout>
  ```

---

#### Task 5: Create ExpenseFragment
**File:** `/ui/fragment/ExpenseFragment.java`

**Features:**
- Month selector (previous/next buttons)
- Total expense display for selected month
- Category breakdown (6 categories):
  - Grocery
  - Utilities
  - Cleaning
  - Gas
  - Rent
  - Miscellaneous
- Recent expenses list (RecyclerView):
  - Category icon
  - Description
  - Amount
  - Date
  - Added by (name)
  - Edit/Delete buttons (admin only)
- FAB to add new expense (navigates to AddExpenseActivity)

**Dependencies:**
- `ExpenseDao.getExpensesByMonth()`
- `ExpenseDao.getTotalExpenseByCategory()`
- `UserDao.getUserById()` (to get name of who added expense)

---

#### Task 6: Create ReportFragment
**File:** `/ui/fragment/ReportFragment.java`

**Features:**
- Month selector
- Monthly summary card:
  - Total expenses (all categories)
  - Total meals consumed (all members)
  - Current meal rate
  - Total collected
  - Total due
- Member balance list (RecyclerView):
  - Member name
  - Total meals
  - Total bill
  - Paid amount
  - Due amount
  - Payment status indicator (green/yellow/red)
- Export report button (optional)
- **Admin only:** Can see all member details
- **Member:** Can see only their own data

**Dependencies:**
- `ReportDao.getMemberBalances()`
- `ReportDao.getTotalExpenses()`
- `ReportDao.calculateMealRate()`
- `UserDao.getUserById()` (check if admin)

---

#### Task 7: Create SettingsFragment
**File:** `/ui/fragment/SettingsFragment.java`

**Features:**
- User profile display:
  - Name
  - Email
  - Phone
  - Role badge (ADMIN/MEMBER)
- Mess information:
  - Mess name
  - Invitation code (with copy button)
  - Member count
  - Joined date
- **Admin only settings:**
  - Update grocery budget per meal
  - Update cooking charge per meal
  - Manage members (view list, remove members)
- General settings:
  - App version
  - About
  - Logout button

**Dependencies:**
- `UserDao.getUserWithMess()`
- `MessDao.getMessById()`
- `MessDao.updateMessRates()` (admin)
- `UserDao.getMembersByMessId()` (admin)

---

### **Phase 6.3: MainActivity Integration**

#### Task 8: Wire Up Bottom Navigation
**File:** `/MainActivity.java`

**Changes:**
1. Add fragment instance variables
2. Initialize fragments in `onCreate()`
3. Implement `setupBottomNavigation()`:
   - Set listener for bottom nav item selection
   - Load default fragment (Dashboard)
4. Create `loadFragment(Fragment fragment)` method:
   - Use FragmentTransaction to replace current fragment
   - Handle fragment backstack

**Code Structure:**
```java
private DashboardFragment dashboardFragment;
private MealFragment mealFragment;
private ExpenseFragment expenseFragment;
private ReportFragment reportFragment;
private SettingsFragment settingsFragment;

private void setupBottomNavigation() {
    // Initialize fragments
    dashboardFragment = new DashboardFragment();
    mealFragment = new MealFragment();
    // ... etc
    
    // Load default fragment
    loadFragment(dashboardFragment);
    
    // Set navigation listener
    bottomNav.setOnItemSelectedListener(item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_dashboard) {
            loadFragment(dashboardFragment);
        } else if (itemId == R.id.nav_meal) {
            loadFragment(mealFragment);
        }
        // ... etc
        return true;
    });
}

private void loadFragment(Fragment fragment) {
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentContainer, fragment)
        .commit();
}
```

---

### **Phase 6.4: RecyclerView Adapters**

#### Task 9: Create Adapter Classes
**Purpose:** Display lists in fragments

**Files to Create:**

1. `/ui/adapter/ExpenseAdapter.java`
   - Binds Expense data to `item_expense.xml`
   - Click listeners for edit/delete

2. `/ui/adapter/MealEntryAdapter.java`
   - Binds Meal data to `item_meal_entry.xml`
   - Calendar view for month

3. `/ui/adapter/MemberBalanceAdapter.java`
   - Binds MemberBalance data to `item_member_balance.xml`
   - Color-coded payment status

4. `/ui/adapter/MemberAdapter.java`
   - Binds User data to `item_member.xml`
   - Admin actions menu

---

### **Phase 6.5: Update AddExpenseActivity**

#### Task 10: Complete AddExpenseActivity
**File:** `/ui/activity/AddExpenseActivity.java`

**Features:**
- Category spinner (6 categories)
- Amount input (number, 2 decimal places)
- Description input (multiline)
- Date picker (default: today)
- Save button
- Validation:
  - Amount > 0
  - Category selected
  - Date not in future

**Dependencies:**
- `ExpenseDao.addExpense()`
- PreferenceManager (get messId, userId)

---

## Implementation Order (Recommended)

### **Week 1: Foundation**
1. ✅ Create all model classes (User, Mess, Expense, Meal, MemberBalance)
2. ✅ Create MealDao, ExpenseDao, ReportDao
3. ✅ Extend UserDao and MessDao with new methods

### **Week 2: Core Fragments**
4. ✅ Create DashboardFragment (basic version - just show user name and mess name)
5. ✅ Wire up MainActivity bottom navigation
6. ✅ Create MealFragment with meal entry (no preference yet)
7. ✅ Update fragment_meal.xml with meal counters

### **Week 3: Advanced Features**
8. ✅ Create ExpenseFragment with list view
9. ✅ Complete AddExpenseActivity
10. ✅ Create ExpenseAdapter
11. ✅ Add meal preference functionality to MealFragment

### **Week 4: Reporting & Settings**
12. ✅ Create ReportFragment
13. ✅ Create MemberBalanceAdapter
14. ✅ Create SettingsFragment
15. ✅ Add admin-only features

### **Week 5: Testing & Polish**
16. ✅ Test complete user flow
17. ✅ Fix bugs
18. ✅ Add loading indicators
19. ✅ Handle edge cases (no data, network errors)
20. ✅ Final integration testing

---

## Critical Decisions

### **Data Flow Pattern:**
```
Fragment → DAO → Database → DAO → Fragment
```

### **Thread Management:**
- Use `MessKhataDatabase.databaseWriteExecutor` for DB operations
- Update UI on main thread using `runOnUiThread()`

### **Session Management:**
- PreferenceManager stores: userId, messId, role
- Check session in each fragment's `onResume()`

### **Error Handling:**
- Toast messages for user feedback
- Logs for debugging
- Graceful fallbacks (show empty state)

---

## String Resources Needed

Add to `/res/values/strings.xml`:
```xml
<string name="sync">Sync</string>
<string name="previous_month">Previous Month</string>
<string name="next_month">Next Month</string>
<string name="total_expenses">Total Expenses</string>
<string name="monthly_summary">Monthly Summary</string>
<string name="profile_picture">Profile Picture</string>
<string name="edit_profile">Edit Profile</string>
<string name="meal_preference_title">Meal Preferences</string>
<string name="meal_preference_breakfast">Breakfast</string>
<string name="meal_preference_lunch">Lunch</string>
<string name="meal_preference_dinner">Dinner</string>
<string name="save_preference">Apply from Tomorrow</string>
<string name="add_expense">Add Expense</string>
<string name="category">Category</string>
<string name="amount">Amount</string>
<string name="description">Description</string>
<string name="date">Date</string>
<string name="save">Save</string>
<string name="cancel">Cancel</string>
<string name="grocery">Grocery</string>
<string name="utilities">Utilities</string>
<string name="cleaning">Cleaning</string>
<string name="gas">Gas</string>
<string name="rent">Rent</string>
<string name="miscellaneous">Miscellaneous</string>
<string name="total_meals">Total Meals</string>
<string name="meal_rate">Meal Rate</string>
<string name="total_bill">Total Bill</string>
<string name="paid">Paid</string>
<string name="due">Due</string>
<string name="payment_status">Payment Status</string>
<string name="member_count">Members</string>
<string name="invitation_code_label">Invitation Code</string>
<string name="copy_code">Copy Code</string>
<string name="grocery_budget">Grocery Budget/Meal</string>
<string name="cooking_charge">Cooking Charge/Meal</string>
<string name="update_rates">Update Rates</string>
<string name="logout">Logout</string>
<string name="about">About</string>
<string name="app_version">Version 1.0</string>
```

---

## Testing Checklist

### **Authentication Flow:**
- [ ] Signup → MessSetup → Dashboard loads
- [ ] Login → Dashboard loads (if has mess)
- [ ] Login → MessSetup (if no mess)
- [ ] Logout → clears session → redirects to login

### **Dashboard:**
- [ ] Shows correct user name
- [ ] Shows mess name and invitation code
- [ ] Displays current month stats
- [ ] Refresh updates data

### **Meal Tracking:**
- [ ] Can add meal for today
- [ ] Counters increment/decrement correctly
- [ ] Can set meal preference
- [ ] Preference applies from tomorrow
- [ ] Monthly summary shows correct total

### **Expense Management:**
- [ ] Can add expense via AddExpenseActivity
- [ ] Expenses appear in list
- [ ] Category filtering works
- [ ] Month navigation works
- [ ] Admin can edit/delete
- [ ] Member cannot edit/delete others' expenses

### **Reports:**
- [ ] Member sees only their data
- [ ] Admin sees all member data
- [ ] Calculations are correct
- [ ] Month navigation works

### **Settings:**
- [ ] Shows correct user profile
- [ ] Invitation code copyable
- [ ] Admin can update rates
- [ ] Logout works

---

## Next Immediate Steps

1. **Start with Phase 6.1, Task 1:** Create all 5 model classes
2. **Then Phase 6.1, Task 2:** Create MealDao, ExpenseDao, ReportDao
3. **Then Phase 6.2, Task 3:** Create basic DashboardFragment
4. **Then Phase 6.3, Task 8:** Wire up MainActivity navigation

This will give you a working skeleton where you can navigate between empty fragments, then fill them in one by one.

---

## Estimated Effort

- **Total Tasks:** 10 major tasks
- **Estimated Time:** 4-5 weeks (part-time development)
- **Lines of Code:** ~3000-4000 LOC
- **Complexity:** Medium (university project level)

**Priority:** 🔴 HIGH - App is currently non-functional without fragments

---

**Do you want me to start implementing Phase 6.1 (model classes + DAO extensions)?**
