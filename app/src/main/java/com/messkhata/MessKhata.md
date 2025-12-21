A mess is a shared living arrangement, usually for:
Students
Working professionals
Bachelors
In mess rooms, meals are shared. So expenses are pretty much tough to put on excel sheet. So we will create messkhata an application tracks and handles the expense or particular mess.

Messkhata database info and user activity:
User: Full Name, Email Address, Phone Number, Password, userId, messId

One user can have one mass per account(can be member, can be admin)
Expenses:

User/admin can add grocery expenses by buying groceries
User/admin can add utilities expenses by paying bill
User/admin can add cleaning expenses by cleaning bill
User/admin can add gas expenses by gas bill
User/admin can add rest expenses by rent bill
User/admin can add Miscellaneous expenses by telling what the expense is.

Users can modify their expense entry.
Admin can delete/modify

Admin can set per meal grocery budget.
Admin can set per meal cooking budget.

But the grocery budget can variable

So in one meal: cooking charge is fixed. But grocery can be more or less. So after fixed pay if grocery takes more money it will add to per meal count.

Example:
Today 12 meal for x member,
Meal fixed rate is 50 (40 + 10). So the today grocery cost is 480.
But grocery cost goes to 600. Means 120 extra . this extra will count in end of month and adjust every meals of that month

So end of the month suppose one user can have 49 per meal. Or 60 per meal and it’s only related to grocery.

But till then the fixed rate will be shown which will set by the mess admin.






.


Users/admin can save their individual meals entry perday how many meal the user need like breakfast 1 or more, Lunch 1 or more, dinner 1 or more. Saved preference like this. If it not changed i will count perday.
Every meal will count 1.

user/or admin can see individual meal count in month and weeks.



In short user can
Admin see:
Month whole cost(utilities, cleaning, grocery, gas, rent, Miscellaneous
Meal cost fixed. But in last day of month is became variable.
Total meal taken
Members count
Set the preferences
See recent entries of meal per day (like today, yesterday)


Can see mess monthly total expense add expenses, see recent expenses

Admin also a member of mess admin can see, save this also but admin can see or set some mess thing.
mess monthly total expense
Total meals
Meal per rate it will dynamic by every day grocery not like user,
Every members monthly need to pay amount, paid amount, can edit

Total collected amount from user current month. Total due of current month
If admin edit anyone pay like 500 out of 1000 paid by faisal. it will count as transaction.























MessKhata - Mess Expense Management System
Project Overview
MessKhata is a shared living expense tracking application for students, working professionals, and bachelors who share meals and living costs.

Core Concepts
1. User & Mess Structure
   Each user belongs to ONE mess
   Users have two roles: Member or Admin
   Admin has all member permissions + management capabilities
2. Meal Pricing Model (Key Feature)
   Fixed Rate = Grocery Budget + Cooking Charge
   Example: 50 TK = 40 TK (grocery) + 10 TK (cooking)
   During the month: Users see fixed rate (e.g., 50 TK/meal)
   End of month: Final rate adjusts based on actual grocery spending
   If groceries cost more → rate increases (e.g., 52 TK/meal)
   If groceries cost less → rate decreases (e.g., 48 TK/meal)
   Cooking charge stays fixed, only grocery varies

Database Schema
Users Table
userId (PK)
fullName
email
phoneNumber
password (hashed)
messId (FK)
role (member/admin)


Mess Table
messId (PK)
messName
groceryBudgetPerMeal (set by admin)
cookingChargePerMeal (set by admin)
createdDate
Expenses Table
expenseId (PK)
messId (FK)
addedBy (userId)
category (grocery/utilities/cleaning/miscellaneous) + new category will be (gas/rent)
amount
description
date
createdAt
modifiedAt
Meals Table
mealId (PK)
userId (FK)
messId (FK)
date
breakfast (count, default 1)
lunch (count, default 1)
dinner (count, default 1)
totalMeals (calculated: breakfast + lunch + dinner)
Payments Table
paymentId (PK)
userId (FK)
messId (FK)
amount
month
year
paidDate
addedBy (admin userId)








User Activities
All Users (Members + Admin)
Expense Management
✅ Add grocery expense (with amount & date)
✅ Add utilities expense (with bill details)
✅ Add cleaning expense (with bill details)
✅ Add miscellaneous expense (with description)
✅ Edit their own expense entries
✅ View recent expenses
Meal Tracking
✅ Set daily meal preference (breakfast/lunch/dinner counts)
Example: Breakfast: 1, Lunch: 1, Dinner: 0
✅ Edit meal count for any day
✅ View personal meal count (daily/weekly/monthly)
✅ View current month's estimated bill (based on fixed rate × meals)
Dashboard View
Total meals taken this month
Estimated amount to pay (meals × fixed rate)
Actual amount paid
Due amount

Admin Activities (Additional)
Mess Configuration
✅ Set grocery budget per meal (e.g., 40 TK)
✅ Set cooking charge per meal (e.g., 10 TK)
✅ Update these rates anytime
Expense Management
✅ Delete any expense entry
✅ Modify any expense entry
Member Management Dashboard
View all members in the mess
See each member's:
Total meals for the month
Calculated bill (final rate × meals)
Amount paid
Due amount
Edit payment records manually
Example: "Faisal paid 500 TK cash" → Admin enters this
Month-End Calculations
✅ View total grocery expense for the month
✅ View total meals consumed (all members combined)
✅ Calculate actual grocery rate per meal:
actualGroceryRate = totalGroceryExpense / totalMeals
✅ Calculate final rate per meal:
finalRate = actualGroceryRate + cookingCharge
✅ Update each member's bill:
memberBill = finalRate × memberMeals
Financial Overview
Total expenses (grocery + utilities + cleaning + misc)
Total meals consumed
Current per-meal rate (fixed during month, final at month-end)
Total amount collected from members
Total due amount across all members

Key Workflows
Workflow 1: Daily Meal Entry
User logs in
Goes to "My Meals"
For today:
Breakfast: 1
Lunch: 1
Dinner: 1
System saves (or uses yesterday's preference if not changed)
Workflow 2: Adding Expense
User buys groceries for 500 TK
Adds expense: Category=Grocery, Amount=500, Date=Today
Expense recorded, shows in recent expenses

Workflow 3: Month-End Settlement (Admin)
Admin views dashboard on last day
System shows:
Total grocery: 15,000 TK
Total meals: 300
Actual grocery rate: 50 TK/meal
Cooking charge: 10 TK/meal
Final rate: 60 TK/meal (was estimated at 50 TK)
Admin sees each member's updated bill
Admin records payments manually
Workflow 4: Payment Recording
Faisal's bill: 1,800 TK (30 meals × 60 TK)
Faisal pays 1,000 TK cash
Admin enters: User=Faisal, Amount=1000, Date=Today
System shows: Paid=1000, Due=800

Identified Issues & Clarifications
Clarified:
Meal count default: Each meal (breakfast/lunch/dinner) = 1 count
Fixed vs Variable rate: Only grocery varies, cooking stays fixed
Payment system: Manual entry by admin (not integrated payment gateway)
Potential Issues for Course Project:
Multiple meals per day type?
Current: User can set "Breakfast: 2" (eating 2 breakfasts)
Is this needed? Or always 0 or 1?
Suggestion: Keep it simple - allow 0, 1, or 2 max
Mid-month rate change by admin?
If admin changes grocery budget from 40 to 45 TK on 15th
How to handle past meals?
Suggestion for simplicity: Rate changes apply from next month
Partial month membership?
User joins mess on 15th
Suggestion: Their bill = finalRate × their meals (automatic)



Month transition?
On 1st of new month, reset all calculations
Previous month becomes "finalized"


Suggested Pages/Views
Member Views
Dashboard: Meals, estimated bill, payments
My Meals: Calendar view to edit daily meals
Add Expense: Form with category/amount
My Expenses: List of added expenses
Admin Views
Admin Dashboard: Mess overview, total expenses, rates
Member Management: Table of all members with bills/payments
Expense Management: All expenses with edit/delete
Settings: Set grocery budget, cooking charge

Does This Match Your Vision?
This structure:
✅ Handles variable grocery costs with month-end adjustment
✅ Keeps cooking charge fixed
✅ Allows meal preferences and counting
✅ Supports manual payment tracking
✅ Gives admin full control
✅ Is feasible for a course project
Any changes needed? Let me know if you want to adjust the meal counting logic, payment flow, or add/remove features!





1. Users Table
   Stores all user information and their mess membership.
   sql
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
   Attributes:
   userId - Primary key, auto-increment
   fullName - User's full name
   email - Unique email for login
   phoneNumber - Unique phone number
   password - Hashed password (bcrypt/argon2)
   messId - Foreign key to Mess table
   role - Either 'member' or 'admin'
   joinedDate - When user created account
   isActive - Soft delete flag




2. Mess Table
   Stores mess information and default rate settings.
   sql
   CREATE TABLE Mess (
   messId                  INT PRIMARY KEY ,
   messName                VARCHAR(100) NOT NULL,
   groceryBudgetPerMeal    DECIMAL(10, 2) NOT NULL DEFAULT 40.00,
   cookingChargePerMeal    DECIMAL(10, 2) NOT NULL DEFAULT 10.00,
   createdDate             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   );
   Attributes:
   messId - Primary key, auto-increment unique 6 alpha digit number also work as invitation code
   messName - Name of the mess (e.g., "Room 301 Mess")
   groceryBudgetPerMeal - Target grocery cost per meal (TK)
   cookingChargePerMeal - Fixed cooking charge per meal (TK)
   createdDate - When mess was created
   isActive - Whether mess is active










3. Expenses Table
   Stores all expense entries (grocery, utilities, cleaning, misc).
   sql
   CREATE TABLE Expenses (
   expenseId       INT PRIMARY KEY AUTO_INCREMENT,
   messId          INT NOT NULL,
   addedBy         INT NOT NULL,
   category        ENUM('grocery', 'utilities', 'cleaning', 'miscellaneous') NOT NULL,
   // need to add new enum
   amount          DECIMAL(10, 2) NOT NULL,
   description     TEXT,
   expenseDate     DATE NOT NULL,
   createdAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updatedAt       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

   FOREIGN KEY (messId) REFERENCES Mess(messId) ON DELETE CASCADE,
   FOREIGN KEY (addedBy) REFERENCES Users(userId) ON DELETE CASCADE
   );
   Attributes:
   expenseId - Primary key, auto-increment
   messId - Which mess this expense belongs to
   addedBy - User who added the expense
   category - Type of expense (grocery/utilities/cleaning/miscellaneous)
   amount - Amount in TK
   description - Details about the expense
   expenseDate - Date of the expense (not when it was entered)
   createdAt - When record was created
   updatedAt - When record was last modified

4. Meals Table
   Stores daily meal counts for each user.
   sql
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
   Attributes:
   mealId - Primary key, auto-increment
   userId - Which user this meal record belongs to
   messId - Which mess
   mealDate - Date of meals
   breakfast - Number of breakfasts (0, 1, 2, 3, etc.)
   lunch - Number of lunches
   dinner - Number of dinners
   totalMeals - Auto-calculated (breakfast + lunch + dinner)
   createdAt - When record was created
   updatedAt - When record was last modified
   UNIQUE constraint: One record per user per day

5. MessMonthlyStats Table
   Stores month-end calculations and statistics for each mess.
   sql
   CREATE TABLE MessMonthlyStats (
   statsId                 INT PRIMARY KEY AUTO_INCREMENT,
   messId                  INT NOT NULL,
   month                   INT NOT NULL,  -- 1-12
   year                    INT NOT NULL,  -- 2024, 2025, etc.

   -- Expense totals
   totalGrocery            DECIMAL(10, 2) DEFAULT 0.00,
   totalUtilities          DECIMAL(10, 2) DEFAULT 0.00,
   totalCleaning           DECIMAL(10, 2) DEFAULT 0.00,
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
   Attributes:
   statsId - Primary key, auto-increment
   messId - Which mess
   month - Month number (1-12)
   year - Year (2024, 2025)
   totalGrocery - Sum of all grocery expenses for the month
   totalUtilities - Sum of all utilities expenses
   totalCleaning - Sum of all cleaning expenses
   totalMiscellaneous - Sum of all miscellaneous expenses
   totalMeals - Total meals consumed by all members
   numberOfMembers - Number of active members that month
   finalGroceryRate - Auto-calculated: totalGrocery / totalMeals
   cookingCharge - Cooking charge (copied from Mess table)
   finalMealRate - Auto-calculated: finalGroceryRate + cookingCharge
   isFinalized - Whether month is closed
   finalizedDate - When month was finalized
   createdAt - When record was created
   UNIQUE constraint: One record per mess per month

6. MonthlyBills Table
   Stores individual member bills for each month.
   sql
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

   -- Shared expenses
   utilitiesShare      DECIMAL(10, 2) DEFAULT 0.00,
   cleaningShare       DECIMAL(10, 2) DEFAULT 0.00,
   miscShare           DECIMAL(10, 2) DEFAULT 0.00,

   -- Total bill
   totalBill           DECIMAL(10, 2) GENERATED ALWAYS AS (
   (totalMeals * mealRate) + utilitiesShare + cleaningShare + miscShare
   ) STORED,

   -- Payment tracking
   totalPaid           DECIMAL(10, 2) DEFAULT 0.00,
   dueAmount           DECIMAL(10, 2) GENERATED ALWAYS AS (
   (totalMeals * mealRate) + utilitiesShare + cleaningShare + miscShare - totalPaid
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
   Attributes:
   billId - Primary key, auto-increment
   userId - Which user this bill belongs to
   messId - Which mess
   month - Month number (1-12)
   year - Year
   totalMeals - User's total meals for the month
   mealRate - Final meal rate for that month
   mealCost - Auto-calculated: totalMeals × mealRate
   utilitiesShare - User's share of utilities (totalUtilities / members)
   cleaningShare - User's share of cleaning
   miscShare - User's share of miscellaneous
   totalBill - Auto-calculated: mealCost + all shares
   totalPaid - Sum of all payments made
   dueAmount - Auto-calculated: totalBill - totalPaid
   status - Payment status (pending/partial/paid)
   finalizedDate - When bill was finalized
   createdAt - When record was created
   updatedAt - When record was last modified
   UNIQUE constraint: One bill per user per month per mess

7. Payments Table
   Stores all payment transactions.
   sql
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

