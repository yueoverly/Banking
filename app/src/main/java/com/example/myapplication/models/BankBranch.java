package com.example.myapplication.models;

import java.io.Serializable;

/**
 * BankBranch model for map navigation feature
 */
public class BankBranch implements Serializable {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String phoneNumber;
    private String workingHours;
    private boolean hasATM;
    private boolean isMainBranch;
    private double distanceFromUser; // in meters
    private String imageUrl;

    public BankBranch() {}

    public BankBranch(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public boolean isHasATM() { return hasATM; }
    public void setHasATM(boolean hasATM) { this.hasATM = hasATM; }

    public boolean isMainBranch() { return isMainBranch; }
    public void setMainBranch(boolean mainBranch) { isMainBranch = mainBranch; }

    public double getDistanceFromUser() { return distanceFromUser; }
    public void setDistanceFromUser(double distanceFromUser) { this.distanceFromUser = distanceFromUser; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getFormattedDistance() {
        if (distanceFromUser < 1000) {
            return String.format("%.0f m", distanceFromUser);
        } else {
            return String.format("%.1f km", distanceFromUser / 1000);
        }
    }

    // Calculate distance from user location using Haversine formula
    public void calculateDistance(double userLat, double userLng) {
        final int R = 6371000; // Earth's radius in meters
        double latDistance = Math.toRadians(latitude - userLat);
        double lonDistance = Math.toRadians(longitude - userLng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        this.distanceFromUser = R * c;
    }
}
