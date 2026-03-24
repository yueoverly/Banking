package com.example.myapplication.models;

import java.util.Date;

public class User {
    
    public enum UserType {
        CUSTOMER,
        OFFICER
    }

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String idCardNumber;
    private String profileImageUrl;
    private UserType userType;
    private boolean isVerified;
    private Date createdAt;
    private Date updatedAt;

    public User() {
        this.userType = UserType.CUSTOMER;
        this.isVerified = false;
    }

    public User(String email, String fullName, String phone, UserType userType) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.userType = userType;
        this.isVerified = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getIdCardNumber() { return idCardNumber; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public UserType getUserType() { return userType; }
    public boolean isVerified() { return isVerified; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
