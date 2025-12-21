package com.messkhata.data.model;

/**
 * User model class
 */
public class User {
    private long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private int messId;
    private String role; // "member" or "admin"
    private long joinedDate; // Unix timestamp

    // Constructor
    public User() {
    }

    public User(long userId, String fullName, String email, String phoneNumber, 
                int messId, String role, long joinedDate) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.messId = messId;
        this.role = role;
        this.joinedDate = joinedDate;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getMessId() {
        return messId;
    }

    public void setMessId(int messId) {
        this.messId = messId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(long joinedDate) {
        this.joinedDate = joinedDate;
    }

    // Helper methods
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean hasMess() {
        return messId > 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", messId=" + messId +
                ", role='" + role + '\'' +
                ", joinedDate=" + joinedDate +
                '}';
    }
}
