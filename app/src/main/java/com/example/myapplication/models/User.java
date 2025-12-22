package com.example.myapplication.models;

import java.io.Serializable;
import java.util.Date;

/**
 * User model representing both bank customers and bank officers
 */
public class User implements Serializable {
    private String id;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String idCardNumber;
    private String address;
    private Date dateOfBirth;
    private String gender;
    private String profileImageUrl;
    private String faceImageUrl;
    private UserType userType;
    private boolean isVerified;
    private boolean isBiometricEnabled;
    private boolean isFaceVerified;
    private boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    private String pin;
    private String fcmToken;

    public enum UserType {
        CUSTOMER,
        OFFICER
    }

    public User() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isVerified = false;
        this.isBiometricEnabled = false;
        this.isFaceVerified = false;
        this.isActive = true;
    }

    public User(String email, String fullName, String phoneNumber, UserType userType) {
        this();
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.userType = userType;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    // Alias for setId
    public void setUserId(String userId) { this.id = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    // Alias for getPhoneNumber
    public String getPhone() { return phoneNumber; }
    public void setPhone(String phone) { this.phoneNumber = phone; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }
    
    // Alias for getIdCardNumber  
    public String getIdCard() { return idCardNumber; }
    public void setIdCard(String idCard) { this.idCardNumber = idCard; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getFaceImageUrl() { return faceImageUrl; }
    public void setFaceImageUrl(String faceImageUrl) { this.faceImageUrl = faceImageUrl; }

    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isBiometricEnabled() { return isBiometricEnabled; }
    public void setBiometricEnabled(boolean biometricEnabled) { isBiometricEnabled = biometricEnabled; }
    
    public boolean isFaceVerified() { return isFaceVerified; }
    public void setFaceVerified(boolean faceVerified) { isFaceVerified = faceVerified; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public boolean isCustomer() {
        return userType == UserType.CUSTOMER;
    }

    public boolean isOfficer() {
        return userType == UserType.OFFICER;
    }
}
