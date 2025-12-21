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
- **During the month:** Users see fixed rate (e.g., 50 TK/meal)
- **End of month:** Final rate adjusts based on actual grocery spending
  - If groceries cost more → rate increases (e.g., 52 TK/meal)
  - If groceries cost less → rate decreases (e.g., 48 TK/meal)
- **Cooking charge stays fixed, only grocery varies**

### 3. Expense Distribution Rules
- **Grocery expenses:** Distributed per meal consumption
- **Utilities/Cleaning/Miscellaneous:** Split equally among all active members
- **Calculation:** `shareAmount = totalExpense / numberOfMembers`

### 4. Meal Preference Auto-Carry
- Users set meal preferences (e.g., Breakfast: 1, Lunch: 1, Dinner: 0)
- **If preference is set before today, it will automatically apply from tomorrow onwards**
- Users can edit any day's meal count manually
- System continues using the last set preference until changed

### 5. Mid-Month Join Handling
- **User joins on 15th of the month:**
  - **Meal billing:** Counted only from days they registered meals (per meal basis)
  - **Fixed expenses (utilities/cleaning/misc):** Split equally among total member count for the entire month
  - System treats them as a member from day 1 for expense splitting purposes

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
- `groceryBudgetPerMeal` - Target grocery cost per meal (TK)
- `cookingChargePerMeal` - Fixed cooking charge per meal (TK)
- `createdDate` - When mess was created
- `isActive` - Whether mess is active

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
- Grocery expenses affect per-meal rate calculation
- Utilities/Cleaning/Gas/Rent/Miscellaneous are split equally among members

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
- If preference is set today, it applies from tomorrow onwards
- System auto-creates Meals records based on preference if user doesn't manually enter

---

### 6. MessMonthlyStats Table
Stores month-end calculations and statistics for each mess.

```sql
CREATE TABLE MessMonthlyStats (
    statsId                 INT PRIMARY KEY AUTO_INCREMENT,
    messId                  INT NOT NULL,
    month                   INT NOT NULL,  -- 1-12
    year                    INT NOT NULL,  -- 2024, 2025, etc.
    
    -- Expense totals
    totalGrocery            DECIMAL(10, 2) DEFAULT 0.00,
    totalUtilities          DECIMAL(10, 2) DEFAULT 0.00,
    totalCleaning           DECIMAL(10, 2) DEFAULT 0.00,
    totalGas                DECIMAL(10, 2) DEFAULT 0.00,
    totalRent               DECIMAL(10, 2) DEFAULT 0.00,
    totalMiscellaneous      DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Meal statistics
    totalMeals              INT DEFAULT 0,  -- All members combined
    numberOfMembers         INT NOT NULL,
    
    -- Calculated rates
    finalGroceryRate        DECIMAL(10, 2) GENERATED ALWAYS AS (
        CASE WHEN totalMeals > 0 THEN totalGrocery / totalMeals ELSE 0 END
    ) STORED,
    cookingCharge           DECIMAL(10, 2) NOT NULL,
    finalMealRate           DECIMAL(10, 2) GENERATED ALWAYS AS (
        CASE WHEN totalMeals > 0 THEN (totalGrocery / totalMeals) + cookingCharge ELSE 0 END
    ) STORED,
    
    -- Status
    isFinalized             BOOLEAN DEFAULT FALSE,
    finalizedDate           TIMESTAMP NULL,
    createdAt               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
    UNIQUE KEY unique_mess_month (messId, month, year)
);
```

**Attributes:**
- `statsId` - Primary key, auto-increment
- `messId` - Which mess
- `month` - Month number (1-12)
- `year` - Year (2024, 2025)
- `totalGrocery` - Sum of all grocery expenses for the month
- `totalUtilities` - Sum of all utilities expenses
- `totalCleaning` - Sum of all cleaning expenses
- `totalGas` - Sum of all gas expenses
- `totalRent` - Sum of all rent expenses
- `totalMiscellaneous` - Sum of all miscellaneous expenses
- `totalMeals` - Total meals consumed by all members
- `numberOfMembers` - Number of active members that month
- `finalGroceryRate` - Auto-calculated: totalGrocery / totalMeals
- `cookingCharge` - Cooking charge (copied from Mess table)
- `finalMealRate` - Auto-calculated: finalGroceryRate + cookingCharge
- `isFinalized` - Whether month is closed
- `finalizedDate` - When month was finalized
- `createdAt` - When record was created
- **UNIQUE constraint:** One record per mess per month

---

### 7. MonthlyBills Table
Stores individual member bills for each month.

```sql
CREATE TABLE MonthlyBills (
    billId              INT PRIMARY KEY AUTO_INCREMENT,
    userId              INT NOT NULL,
    messId              INT NOT NULL,
    month               INT NOT NULL,  -- 1-12
    year                INT NOT NULL,  -- 2024, 2025
    
    -- Meal calculations
    totalMeals          INT NOT NULL,
    mealRate            DECIMAL(10, 2) NOT NULL,
    mealCost            DECIMAL(10, 2) GENERATED ALWAYS AS (totalMeals * mealRate) STORED,
    
    -- Shared expenses (split equally among members)
    utilitiesShare      DECIMAL(10, 2) DEFAULT 0.00,
    cleaningShare       DECIMAL(10, 2) DEFAULT 0.00,
    gasShare            DECIMAL(10, 2) DEFAULT 0.00,
    rentShare           DECIMAL(10, 2) DEFAULT 0.00,
    miscShare           DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Total bill
    totalBill           DECIMAL(10, 2) GENERATED ALWAYS AS (
        (totalMeals * mealRate) + utilitiesShare + cleaningShare + gasShare + rentShare + miscShare
    ) STORED,
    
    -- Payment tracking
    totalPaid           DECIMAL(10, 2) DEFAULT 0.00,
    dueAmount           DECIMAL(10, 2) GENERATED ALWAYS AS (
        (totalMeals * mealRate) + utilitiesShare + cleaningShare + gasShare + rentShare + miscShare - totalPaid
    ) STORED,
    
    -- Status
    status              ENUM('pending', 'partial', 'paid') DEFAULT 'pending',
    finalizedDate       TIMESTAMP NULL,
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
- `totalMeals` - User's total meals for the month
- `mealRate` - Final meal rate for that month
- `mealCost` - Auto-calculated: totalMeals × mealRate
- `utilitiesShare` - User's share: totalUtilities / numberOfMembers
- `cleaningShare` - User's share: totalCleaning / numberOfMembers
- `gasShare` - User's share: totalGas / numberOfMembers
- `rentShare` - User's share: totalRent / numberOfMembers
- `miscShare` - User's share: totalMiscellaneous / numberOfMembers
- `totalBill` - Auto-calculated: mealCost + all shares
- `totalPaid` - Sum of all payments made
- `dueAmount` - Auto-calculated: totalBill - totalPaid
- `status` - Payment status (pending/partial/paid)
- `finalizedDate` - When bill was finalized
- `createdAt` - When record was created
- `updatedAt` - When record was last modified
- **UNIQUE constraint:** One bill per user per month per mess

**Note:** Fixed expenses are split equally regardless of when user joined in the month.

---

### 8. Payments Table
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
✅ Set meal preference (breakfast/lunch/dinner counts) - applies from tomorrow  
✅ Edit meal count for any specific day  
✅ View personal meal count (daily/weekly/monthly)  
✅ View current month's estimated bill (based on fixed rate × meals)  

#### Dashboard View
- Total meals taken this month
- Estimated amount to pay (meals × fixed rate)
- Actual amount paid
- Due amount

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

#### Month-End Calculations
✅ View total grocery expense for the month  
✅ View total meals consumed (all members combined)  
✅ Calculate actual grocery rate per meal:  
   `actualGroceryRate = totalGroceryExpense / totalMeals`  
✅ Calculate final rate per meal:  
   `finalRate = actualGroceryRate + cookingCharge`  
✅ Update each member's bill:  
   `memberBill = (finalRate × memberMeals) + memberShareOfFixedExpenses`  

#### Financial Overview
- Total expenses (grocery + utilities + cleaning + gas + rent + misc)
- Total meals consumed
- Current per-meal rate (fixed during month, final at month-end)
- Total amount collected from members
- Total due amount across all members

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
2. Sets: Breakfast: 1, Lunch: 0, Dinner: 1
3. Preference saved with `effectiveFrom = tomorrow's date`
4. From tomorrow onwards, system auto-creates meal entries with this preference
5. User can still manually edit any day

### Workflow 3: Adding Expense
1. User buys groceries for 500 TK
2. Adds expense: Category=Grocery, Amount=500, Date=Today
3. Expense recorded, shows in recent expenses
4. Admin can view this in expense management

### Workflow 4: Mid-Month User Join
1. User joins mess on 15th January
2. System creates user account with `joinedDate = 15th Jan`
3. User sets meal preference: Breakfast: 1, Lunch: 1, Dinner: 0
4. User's meals counted from 15th onwards
5. At month-end:
   - User's meal bill = their total meals × final meal rate
   - User's fixed expense share = (totalUtilities + totalCleaning + totalGas + totalRent + totalMisc) / totalMembers
   - Total bill = meal bill + fixed expense share

### Workflow 5: Month-End Settlement (Admin)
1. Admin views dashboard on last day
2. System shows:
   - Total grocery: 15,000 TK
   - Total meals: 300
   - Actual grocery rate: 50 TK/meal
   - Cooking charge: 10 TK/meal
   - Final rate: 60 TK/meal (was estimated at 50 TK)
3. Admin sees each member's updated bill
4. Admin records payments manually

### Workflow 6: Payment Recording
1. Faisal's bill: 1,800 TK (30 meals × 60 TK)
2. Faisal pays 1,000 TK cash
3. Admin enters: User=Faisal, Amount=1000, Date=Today
4. System shows: Paid=1000, Due=800

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
4. **Manual month finalization** - Admin clicks "Finalize Month" button
5. **Basic UI** - Android Material Design components
6. **Local database** - Room Database (SQLite wrapper for Android)
7. **Firebase Backend** - For real-time sync between members (optional)

### Key Features for Demo:
✅ User signup/login  
✅ Create/join mess using invitation code  
✅ Add expenses (all categories)  
✅ Record daily meals  
✅ Set meal preferences  
✅ View personal dashboard  
✅ Admin: View all members and bills  
✅ Admin: Record payments  
✅ Month-end calculation  

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
✅ Variable grocery cost with month-end adjustment  
✅ Fixed cooking charge  
✅ Meal preference auto-carry functionality  
✅ Clear expense distribution rules (per meal vs equal split)  
✅ Mid-month join handling  
✅ Manual payment tracking  
✅ Admin control  
✅ Feasible for university course project  

**Database Tables:** 8 tables with clear relationships  
**User Roles:** 2 (Member, Admin)  
**Expense Categories:** 6 (Grocery, Utilities, Cleaning, Gas, Rent, Miscellaneous)  
**Meal Types:** 3 (Breakfast, Lunch, Dinner)

