# MessKhata - Mess Expense Management System

## Project Overview
MessKhata is a shared living expense tracking application for students, working professionals, and bachelors who share meals and living costs in a mess.

---

## Core Concepts

### 1. User & Mess Structure
- Each user belongs to **ONE mess**
- Users have two roles: **Member** or **Admin**
- Admin has all member permissions + management capabilities

### 2. Meal Pricing Model (Key Feature)
- **Fixed Rate** = Grocery Budget + Cooking Charge
- **Example:** 50 TK = 40 TK (grocery) + 10 TK (cooking)
- **Rate is FIXED throughout the month** - no adjustment at month-end
- Admin sets the meal rate (e.g., 50 TK/meal)
- When user consumes meals, the cost is added to their personal expense immediately
- **Example:** User eats 3 meals (breakfast, lunch, dinner) = 3 × 50 TK = 150 TK added to personal expense

### 3. Expense Distribution Rules
- **Meal expenses:** Charged per meal at fixed rate (set by admin)
  - Each meal consumed adds: (groceryRate + cookingCharge) to personal expense
  - Example: 3 meals × 50 TK = 150 TK added immediately
- **All other expenses (Grocery, Utilities, Cleaning, Gas, Rent, Miscellaneous):**
  - Any member can add these expenses
  - Expenses are added to mess total expenses
  - **Automatically split equally among ALL active members**
  - Each member's share: `shareAmount = totalExpense / numberOfMembers`
  - Individual share is added to each member's personal total expense dynamically

### 4. Meal Preference Auto-Charge
- Users set meal preferences (e.g., Breakfast: 1, Lunch: 1, Dinner: 1)
- **When preference is set TODAY:**
  - Meal expense for today is charged IMMEDIATELY (e.g., 3 meals × 50 TK = 150 TK)
  - Preference applies from tomorrow onwards automatically
- **From tomorrow onwards:**
  - If user doesn't change preference, same meals are auto-charged daily (e.g., 150 TK/day)
  - User can manually edit any day's meal count
- System continues using the last set preference until changed

### 5. Mid-Month Join Handling
- **User joins on 15th of the month:**
  - **Meal expenses:** Charged only for meals they consume from join date onwards
  - **All other expenses (grocery/utilities/cleaning/gas/rent/misc):**
    - User is counted in member count from join date onwards
    - Only expenses added AFTER join date are split with this user
    - Past expenses (before join date) are split among members who were active then

### 6. Mess ID & Invitation Code
- **messId:** Auto-increment primary key
- **Invitation Code:** Unique 6-digit numeric code (e.g., 123456)
- This code is used for members to join an existing mess

---

## Database Schema

### 1. Users Table
Stores all user information and their mess membership.

```sql
CREATE TABLE Users (
    userId          INT PRIMARY KEY AUTO_INCREMENT,
    fullName        VARCHAR(100) NOT NULL,
    email           VARCHAR(100) UNIQUE NOT NULL,
    phoneNumber     VARCHAR(20) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,  -- hashed password
    messId          INT,
    role            ENUM('member', 'admin') DEFAULT 'member',
    joinedDate      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    isActive        BOOLEAN DEFAULT TRUE,
    
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE SET NULL
);
```

**Attributes:**
- `userId` - Primary key, auto-increment
- `fullName` - User's full name
- `email` - Unique email for login
- `phoneNumber` - Unique phone number
- `password` - Hashed password (bcrypt recommended)
- `messId` - Foreign key to Mess table
- `role` - Either 'member' or 'admin'
- `joinedDate` - When user created account
- `isActive` - Soft delete flag

---

### 2. Mess Table
Stores mess information and default rate settings.

```sql
CREATE TABLE Mess (
    messId                  INT PRIMARY KEY AUTO_INCREMENT,
    messName                VARCHAR(100) NOT NULL,
    invitationCode          VARCHAR(6) UNIQUE NOT NULL,  -- 6-digit numeric code
    groceryBudgetPerMeal    DECIMAL(10, 2) NOT NULL DEFAULT 40.00,
    cookingChargePerMeal    DECIMAL(10, 2) NOT NULL DEFAULT 10.00,
    createdDate             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    isActive                BOOLEAN DEFAULT TRUE
);
```

**Attributes:**
- `messId` - Primary key, auto-increment
- `messName` - Name of the mess (e.g., "Room 301 Mess")
- `invitationCode` - Unique 6-digit numeric code for joining mess
- `groceryBudgetPerMeal` - Fixed grocery rate per meal (TK) - set by admin
- `cookingChargePerMeal` - Fixed cooking charge per meal (TK) - set by admin
- `createdDate` - When mess was created
- `isActive` - Whether mess is active

**Note:** Total meal rate = groceryBudgetPerMeal + cookingChargePerMeal (e.g., 40 + 10 = 50 TK/meal)

---

### 3. Expenses Table
Stores all expense entries (grocery, utilities, cleaning, gas, rent, misc).

```sql
CREATE TABLE Expenses (
    expenseId       INT PRIMARY KEY AUTO_INCREMENT,
    messId          INT NOT NULL,
    addedBy         INT NOT NULL,
    category        ENUM('grocery', 'utilities', 'cleaning', 'gas', 'rent', 'miscellaneous') NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    description     TEXT,
    expenseDate     DATE NOT NULL,
    createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
    FOREIGN KEY (addedBy) REFERENCES Users(userId) ON DELETE CASCADE
);
```

**Attributes:**
- `expenseId` - Primary key, auto-increment
- `messId` - Which mess this expense belongs to
- `addedBy` - User who added the expense
- `category` - Type of expense (grocery/utilities/cleaning/gas/rent/miscellaneous)
- `amount` - Amount in TK
- `description` - Details about the expense
- `expenseDate` - Date of the expense (not when it was entered)
- `createdAt` - When record was created
- `updatedAt` - When record was last modified

**Note:** 
- All expenses (grocery/utilities/cleaning/gas/rent/miscellaneous) are split equally among active members
- When any member adds an expense, it is automatically distributed to all members
- Each member's share = totalExpense / numberOfMembers

---

### 4. Meals Table
Stores daily meal counts for each user.

```sql
CREATE TABLE Meals (
    mealId          INT PRIMARY KEY AUTO_INCREMENT,
    userId          INT NOT NULL,
    messId          INT NOT NULL,
    mealDate        DATE NOT NULL,
    breakfast       INT DEFAULT 1,
    lunch           INT DEFAULT 1,
    dinner          INT DEFAULT 1,
    totalMeals      INT GENERATED ALWAYS AS (breakfast + lunch + dinner) STORED,
    mealRate        DECIMAL(10, 2) NOT NULL,  -- Fixed rate at time of consumption
    mealExpense     DECIMAL(10, 2) GENERATED ALWAYS AS (totalMeals * mealRate) STORED,
    createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE,
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (userId, mealDate)
);
```

**Attributes:**
- `mealId` - Primary key, auto-increment
- `userId` - Which user this meal record belongs to
- `messId` - Which mess
- `mealDate` - Date of meals
- `breakfast` - Number of breakfasts (0, 1, 2, etc.)
- `lunch` - Number of lunches
- `dinner` - Number of dinners
- `totalMeals` - Auto-calculated (breakfast + lunch + dinner)
- `mealRate` - Fixed meal rate at time of consumption (groceryBudget + cookingCharge)
- `mealExpense` - Auto-calculated (totalMeals × mealRate) - added to personal expense
- `createdAt` - When record was created
- `updatedAt` - When record was last modified
- **UNIQUE constraint:** One record per user per day

---

### 5. MealPreferences Table
Stores user's default meal preferences for auto-carry functionality.

```sql
CREATE TABLE MealPreferences (
    preferenceId    INT PRIMARY KEY AUTO_INCREMENT,
    userId          INT NOT NULL,
    messId          INT NOT NULL,
    breakfast       INT DEFAULT 1,
    lunch           INT DEFAULT 1,
    dinner          INT DEFAULT 1,
    effectiveFrom   DATE NOT NULL,
    createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE,
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE
);
```

**Attributes:**
- `preferenceId` - Primary key, auto-increment
- `userId` - Which user
- `messId` - Which mess
- `breakfast/lunch/dinner` - Default counts for each meal
- `effectiveFrom` - Date from which this preference applies (tomorrow if set today)
- `createdAt` - When preference was set

**Logic:**
- When preference is set today, meal expense for today is charged immediately
- Preference applies from tomorrow onwards automatically
- System auto-creates Meals records and charges based on preference daily
- User can manually edit any day's meal count

---

### 6. MessMonthlyStats Table
Stores monthly statistics and totals for each mess (for reporting purposes).

```sql
CREATE TABLE MessMonthlyStats (
    statsId                 INT PRIMARY KEY AUTO_INCREMENT,
    messId                  INT NOT NULL,
    month                   INT NOT NULL,  -- 1-12
    year                    INT NOT NULL,  -- 2024, 2025, etc.
    
    -- Expense totals (sum of all expenses added)
    totalGrocery            DECIMAL(10, 2) DEFAULT 0.00,
    totalUtilities          DECIMAL(10, 2) DEFAULT 0.00,
    totalCleaning           DECIMAL(10, 2) DEFAULT 0.00,
    totalGas                DECIMAL(10, 2) DEFAULT 0.00,
    totalRent               DECIMAL(10, 2) DEFAULT 0.00,
    totalMiscellaneous      DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Meal statistics
    totalMealExpenses       DECIMAL(10, 2) DEFAULT 0.00,  -- All members' meal expenses combined
    totalMealsConsumed      INT DEFAULT 0,  -- All members' meals combined
    numberOfMembers         INT NOT NULL,
    
    createdAt               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt               TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
    UNIQUE KEY unique_mess_month (messId, month, year)
);
```

**Attributes:**
- `statsId` - Primary key, auto-increment
- `messId` - Which mess
- `month` - Month number (1-12)
- `year` - Year (2024, 2025)
- `totalGrocery` - Sum of all grocery expenses added this month
- `totalUtilities` - Sum of all utilities expenses added this month
- `totalCleaning` - Sum of all cleaning expenses added this month
- `totalGas` - Sum of all gas expenses added this month
- `totalRent` - Sum of all rent expenses added this month
- `totalMiscellaneous` - Sum of all miscellaneous expenses added this month
- `totalMealExpenses` - Sum of all members' meal expenses for the month
- `totalMealsConsumed` - Total meals consumed by all members
- `numberOfMembers` - Number of active members that month
- `createdAt` - When record was created
- `updatedAt` - When record was last modified
- **UNIQUE constraint:** One record per mess per month

**Note:** This table is for reporting/statistics only. Actual expenses are tracked in real-time.

---

### 7. PersonalExpenses Table
Tracks each user's personal expense ledger in real-time.

```sql
CREATE TABLE PersonalExpenses (
    personalExpenseId   INT PRIMARY KEY AUTO_INCREMENT,
    userId              INT NOT NULL,
    messId              INT NOT NULL,
    expenseType         ENUM('meal', 'grocery', 'utilities', 'cleaning', 'gas', 'rent', 'miscellaneous') NOT NULL,
    amount              DECIMAL(10, 2) NOT NULL,
    description         TEXT,
    relatedId           INT,  -- mealId or expenseId depending on type
    expenseDate         DATE NOT NULL,
    createdAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE,
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE
);
```

**Attributes:**
- `personalExpenseId` - Primary key, auto-increment
- `userId` - Which user this expense belongs to
- `messId` - Which mess
- `expenseType` - Type of expense (meal/grocery/utilities/cleaning/gas/rent/miscellaneous)
- `amount` - Amount charged to this user
- `description` - Details (e.g., "3 meals @ 50 TK" or "Grocery share")
- `relatedId` - Links to Meals.mealId or Expenses.expenseId
- `expenseDate` - Date of the expense
- `createdAt` - When record was created

**Logic:**
- **Meal expenses:** When user consumes meals, individual charge added (e.g., 3 meals × 50 = 150 TK)
- **Other expenses:** When anyone adds grocery/utilities/etc, each member gets their share added automatically
- Total personal expense = SUM of all amounts for that user

---

### 8. MonthlyBills Table
Stores simplified monthly bill summaries for each user.

```sql
CREATE TABLE MonthlyBills (
    billId              INT PRIMARY KEY AUTO_INCREMENT,
    userId              INT NOT NULL,
    messId              INT NOT NULL,
    month               INT NOT NULL,  -- 1-12
    year                INT NOT NULL,  -- 2024, 2025
    
    -- Expense breakdown
    totalMealExpense    DECIMAL(10, 2) DEFAULT 0.00,
    totalOtherExpenses  DECIMAL(10, 2) DEFAULT 0.00,  -- Sum of all shared expenses
    
    -- Total bill
    totalBill           DECIMAL(10, 2) GENERATED ALWAYS AS (totalMealExpense + totalOtherExpenses) STORED,
    
    -- Payment tracking
    totalPaid           DECIMAL(10, 2) DEFAULT 0.00,
    dueAmount           DECIMAL(10, 2) GENERATED ALWAYS AS (totalMealExpense + totalOtherExpenses - totalPaid) STORED,
    
    -- Status
    status              ENUM('pending', 'partial', 'paid') DEFAULT 'pending',
    createdAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt           TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE,
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
    UNIQUE KEY unique_user_month (userId, messId, month, year)
);
```

**Attributes:**
- `billId` - Primary key, auto-increment
- `userId` - Which user this bill belongs to
- `messId` - Which mess
- `month` - Month number (1-12)
- `year` - Year
- `totalMealExpense` - Sum of all meal expenses for the month (from PersonalExpenses)
- `totalOtherExpenses` - Sum of all other expenses (grocery/utilities/cleaning/gas/rent/misc shares)
- `totalBill` - Auto-calculated: totalMealExpense + totalOtherExpenses
- `totalPaid` - Sum of all payments made
- `dueAmount` - Auto-calculated: totalBill - totalPaid
- `status` - Payment status (pending/partial/paid)
- `createdAt` - When record was created
- `updatedAt` - When record was last modified
- **UNIQUE constraint:** One bill per user per month per mess

**Note:** This table is updated dynamically as expenses are added throughout the month.

---

### 9. Payments Table
Stores all payment transactions.

```sql
CREATE TABLE Payments (
    paymentId       INT PRIMARY KEY AUTO_INCREMENT,
    billId          INT NOT NULL,
    userId          INT NOT NULL,
    messId          INT NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    paidDate        DATE NOT NULL,
    addedBy         INT NOT NULL,  -- Admin who recorded payment
    paymentMethod   VARCHAR(50) DEFAULT 'Cash',  -- Cash, Bank Transfer, etc.
    notes           TEXT,
    createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (billId) REFERENCES MonthlyBills(billId) ON DELETE CASCADE,
    FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE,
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
    FOREIGN KEY (addedBy) REFERENCES Users(userId) ON DELETE CASCADE
);
```

**Attributes:**
- `paymentId` - Primary key, auto-increment
- `billId` - Which bill this payment is for
- `userId` - Who made the payment
- `messId` - Which mess
- `amount` - Payment amount in TK
- `paidDate` - Date of payment
- `addedBy` - Admin who recorded this payment
- `paymentMethod` - How payment was made (Cash/Bank/etc.)
- `notes` - Optional notes
- `createdAt` - When record was created

---

## User Activities

### All Users (Members + Admin)

#### Expense Management
✅ Add grocery expense (with amount & date)  
✅ Add utilities expense (with bill details)  
✅ Add cleaning expense (with bill details)  
✅ Add gas expense (with bill details)  
✅ Add rent expense (with bill details)  
✅ Add miscellaneous expense (with description)  
✅ Edit their own expense entries  
✅ View recent expenses  

#### Meal Tracking
✅ Set meal preference (breakfast/lunch/dinner counts)
  - Charged immediately for today (e.g., 3 meals × 50 TK = 150 TK)
  - Auto-applies from tomorrow onwards
✅ Edit meal count for any specific day (charges adjusted automatically)
✅ View personal meal count (daily/weekly/monthly)
✅ View current month's total meal expense (sum of all meal charges)  

#### Dashboard View
- Total meals taken this month
- Total meal expense (meals × fixed rate)
- Total shared expenses (grocery/utilities/cleaning/gas/rent/misc shares)
- Total amount to pay (meal expense + shared expenses)
- Actual amount paid
- Due amount (updated in real-time)

---

### Admin Activities (Additional)

#### Mess Configuration
✅ Set grocery budget per meal (e.g., 40 TK)  
✅ Set cooking charge per meal (e.g., 10 TK)  
✅ Update these rates anytime  
✅ Generate and view mess invitation code  

#### Expense Management
✅ Delete any expense entry  
✅ Modify any expense entry  

#### Member Management Dashboard
- View all members in the mess
- See each member's:
  - Total meals for the month
  - Calculated bill (final rate × meals + shared expenses)
  - Amount paid
  - Due amount
- Edit payment records manually  
  - Example: "Faisal paid 500 TK cash" → Admin enters this

#### Monthly Overview
✅ View total expenses by category (grocery/utilities/cleaning/gas/rent/misc)
✅ View total meals consumed (all members combined)
✅ View total meal expenses collected
✅ View each member's current balance (total expense vs paid)
✅ Export monthly report  

#### Financial Overview
- Total mess expenses (grocery + utilities + cleaning + gas + rent + misc)
- Total meal expenses (all members combined)
- Current fixed meal rate (set by admin)
- Total amount collected from members (all payments)
- Total due amount across all members (updated in real-time)

---

## Key Workflows

### Workflow 1: Daily Meal Entry
1. User logs in
2. Goes to "My Meals"
3. For today:
   - Breakfast: 1
   - Lunch: 1
   - Dinner: 1
4. System saves the meal count
5. If preference was set earlier, tomorrow's meal will auto-populate with that preference

### Workflow 2: Setting Meal Preference
1. User goes to "Meal Preferences"
2. Sets: Breakfast: 1, Lunch: 1, Dinner: 1 (Total: 3 meals)
3. System immediately:
   - Charges today: 3 × 50 TK = 150 TK added to personal expense
   - Saves preference with `effectiveFrom = tomorrow's date`
4. From tomorrow onwards:
   - System auto-charges 150 TK daily (if preference unchanged)
   - User can manually edit any day's meal count (charges adjust automatically)

### Workflow 3: Adding Expense (Auto-Distribution)
1. User (any member) buys groceries for 600 TK
2. Adds expense: Category=Grocery, Amount=600, Date=Today
3. System automatically:
   - Adds 600 TK to mess total grocery expense
   - Gets current member count (e.g., 4 members)
   - Calculates share: 600 / 4 = 150 TK per member
   - Adds 150 TK to each member's personal expense
   - Creates PersonalExpense record for each member
4. All members see updated total expense in their dashboard immediately

### Workflow 4: Mid-Month User Join
1. User joins mess on 15th January (4 existing members)
2. System creates user account with `joinedDate = 15th Jan`
3. User sets meal preference: Breakfast: 1, Lunch: 1, Dinner: 1
4. Meal expense charged: 3 × 50 = 150 TK (added to personal expense)
5. For expenses added BEFORE 15th Jan:
   - Split among 4 members only (new user not charged)
6. For expenses added ON/AFTER 15th Jan:
   - Split among 5 members (including new user)
   - Example: 500 TK grocery added on 16th Jan → Each gets 500/5 = 100 TK

### Workflow 5: Real-Time Expense Tracking
1. Throughout the month, expenses are tracked in real-time:
   - **Day 1:** Faisal sets preference (3 meals/day) → 150 TK charged
   - **Day 2:** Auto-charged 150 TK (preference continues)
   - **Day 5:** Someone adds 400 TK utilities → Each member gets 100 TK share
   - **Day 10:** Faisal changes preference to 2 meals/day → 100 TK charged
2. Faisal's dashboard shows:
   - Meal expense: 1,200 TK (8 days × 150 + 2 days × 100)
   - Shared expenses: 300 TK (various splits)
   - Total: 1,500 TK
   - Paid: 1,000 TK
   - Due: 500 TK

### Workflow 6: Payment Recording
1. Faisal's current bill: 1,500 TK (meal expenses + shared expenses)
2. Faisal pays 1,000 TK cash to admin
3. Admin records payment:
   - User: Faisal
   - Amount: 1,000 TK
   - Date: Today
   - Method: Cash
4. System updates:
   - Faisal's totalPaid: 1,000 TK
   - Faisal's dueAmount: 500 TK
   - Status: Partial (since due > 0)

---

## Suggested Pages/Views

### Member Views
- **Dashboard:** Meals, estimated bill, payments
- **My Meals:** Calendar view to edit daily meals
- **Meal Preferences:** Set default meal preferences
- **Add Expense:** Form with category/amount
- **My Expenses:** List of added expenses

### Admin Views
- **Admin Dashboard:** Mess overview, total expenses, rates
- **Member Management:** Table of all members with bills/payments
- **Expense Management:** All expenses with edit/delete
- **Settings:** Set grocery budget, cooking charge, view invitation code

---

## Implementation Notes for University Project

### Simplifications for Course Project:
1. **No payment gateway integration** - Manual cash/bank entry by admin
2. **Basic authentication** - Email/password with simple hashing (bcrypt)
3. **Single currency** - TK (Taka) only
4. **Real-time expense tracking** - No complex month-end calculations needed
5. **Basic UI** - Android Material Design components
6. **Local database** - Room Database (SQLite wrapper for Android)
7. **Firebase Backend** - For real-time sync between members (optional)

### Key Features for Demo:
✅ User signup/login  
✅ Create/join mess using invitation code  
✅ Add expenses (all categories) with auto-distribution  
✅ Record daily meals with immediate charging  
✅ Set meal preferences with auto-charging  
✅ View personal dashboard with real-time expense tracking  
✅ Admin: Set fixed meal rate (grocery + cooking)  
✅ Admin: View all members and real-time balances  
✅ Admin: Record payments  
✅ Real-time expense distribution and tracking  

### Out of Scope (Keep Simple):
❌ Multi-mess support per user  
❌ Advanced analytics/charts  
❌ Push notifications  
❌ Chat between members  
❌ Automated payment reminders  
❌ Historical data beyond 3 months  

---

## Summary

This plan provides:
✅ Fixed meal rate (no month-end adjustment)
✅ Immediate meal expense charging (when preference set or meals consumed)
✅ Automatic expense distribution (all categories split equally among members)
✅ Real-time personal expense tracking
✅ Meal preference with auto-charging functionality
✅ Mid-month join handling with fair expense sharing
✅ Manual payment tracking
✅ Admin control over meal rates
✅ Simplified implementation - feasible for university course project

**Database Tables:** 9 tables with clear relationships
**User Roles:** 2 (Member, Admin)
**Expense Categories:** 6 (Grocery, Utilities, Cleaning, Gas, Rent, Miscellaneous) + Meals
**Meal Types:** 3 (Breakfast, Lunch, Dinner)
**Key Feature:** Real-time expense tracking with automatic distribution

