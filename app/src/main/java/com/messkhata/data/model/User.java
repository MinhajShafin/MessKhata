package com.messkhata.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User entity representing a member of the mess.
 * Roles: ADMIN, MANAGER, MEMBER
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    private String id;

    private String email;
    private String name;
    private String phone;
    private String role; // ADMIN, MANAGER, MEMBER
    private String messId;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;
    private String pendingAction; // CREATE, UPDATE, DELETE, null if synced

    public User() {
        this.id = "";
    }

    public User(@NonNull String id, String email, String name, String phone,
                String role, String messId) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.messId = messId;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isSynced = false;
        this.pendingAction = "CREATE";
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessId() { return messId; }
    public void setMessId(String messId) { this.messId = messId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }

    // Role check helpers
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isManager() { return "MANAGER".equals(role); }
    public boolean isMember() { return "MEMBER".equals(role); }
}
