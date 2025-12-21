package com.messkhata.data.model;

/**
 * MemberBalance model class
 * Used for displaying member bills and payment status in reports
 */
public class MemberBalance {
    private long userId;
    private String fullName;
    private int totalMeals;
    private double totalBill;
    private double totalPaid;
    private double dueAmount;
    private String paymentStatus; // "paid", "partial", "pending"

    // Constructor
    public MemberBalance() {
    }

    public MemberBalance(long userId, String fullName, int totalMeals, 
                        double totalBill, double totalPaid) {
        this.userId = userId;
        this.fullName = fullName;
        this.totalMeals = totalMeals;
        this.totalBill = totalBill;
        this.totalPaid = totalPaid;
        this.dueAmount = totalBill - totalPaid;
        this.paymentStatus = calculatePaymentStatus();
    }

    // Getters and Setters
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getTotalMeals() {
        return totalMeals;
    }

    public void setTotalMeals(int totalMeals) {
        this.totalMeals = totalMeals;
    }

    public double getTotalBill() {
        return totalBill;
    }

    public void setTotalBill(double totalBill) {
        this.totalBill = totalBill;
        this.dueAmount = totalBill - totalPaid;
        this.paymentStatus = calculatePaymentStatus();
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(double totalPaid) {
        this.totalPaid = totalPaid;
        this.dueAmount = totalBill - totalPaid;
        this.paymentStatus = calculatePaymentStatus();
    }

    public double getDueAmount() {
        return dueAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    // Helper method to calculate payment status
    private String calculatePaymentStatus() {
        if (totalPaid >= totalBill) {
            return "paid";
        } else if (totalPaid > 0) {
            return "partial";
        } else {
            return "pending";
        }
    }

    // Helper method to get payment percentage
    public int getPaymentPercentage() {
        if (totalBill <= 0) return 0;
        return (int) ((totalPaid / totalBill) * 100);
    }

    @Override
    public String toString() {
        return "MemberBalance{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", totalMeals=" + totalMeals +
                ", totalBill=" + totalBill +
                ", totalPaid=" + totalPaid +
                ", dueAmount=" + dueAmount +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}
