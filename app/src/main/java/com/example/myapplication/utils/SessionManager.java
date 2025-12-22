package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.example.myapplication.models.User;

/**
 * Session manager for handling user login/logout and preferences
 */
public class SessionManager {
    private static SessionManager instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    private static final String KEY_USER = "key_user";
    private static final String KEY_FCM_TOKEN = "key_fcm_token";

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // Alias for backward compatibility
    public void saveFcmToken(String token) {
        setFcmToken(token);
    }

    // ==================== Login/Logout ====================

    public void createLoginSession(User user) {
        editor.putBoolean(Constants.PREF_IS_LOGGED_IN, true);
        editor.putString(Constants.PREF_USER_ID, user.getId());
        editor.putString(Constants.PREF_USER_TYPE, user.getUserType().name());
        editor.putString(KEY_USER, gson.toJson(user));
        editor.apply();
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(Constants.PREF_IS_LOGGED_IN, false);
    }

    // ==================== User Data ====================

    public String getUserId() {
        return pref.getString(Constants.PREF_USER_ID, null);
    }

    public User.UserType getUserType() {
        String type = pref.getString(Constants.PREF_USER_TYPE, null);
        if (type != null) {
            return User.UserType.valueOf(type);
        }
        return null;
    }

    public User getUser() {
        String userJson = pref.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public void updateUser(User user) {
        editor.putString(KEY_USER, gson.toJson(user));
        editor.apply();
    }

    public boolean isCustomer() {
        return getUserType() == User.UserType.CUSTOMER;
    }

    public boolean isOfficer() {
        return getUserType() == User.UserType.OFFICER;
    }

    // ==================== Biometric Settings ====================

    public void setBiometricEnabled(boolean enabled) {
        editor.putBoolean(Constants.PREF_BIOMETRIC_ENABLED, enabled);
        editor.apply();
    }

    public boolean isBiometricEnabled() {
        return pref.getBoolean(Constants.PREF_BIOMETRIC_ENABLED, false);
    }

    // ==================== PIN Settings ====================

    public void setPinEnabled(boolean enabled) {
        editor.putBoolean(Constants.PREF_PIN_ENABLED, enabled);
        editor.apply();
    }

    public boolean isPinEnabled() {
        return pref.getBoolean(Constants.PREF_PIN_ENABLED, false);
    }

    // ==================== FCM Token ====================

    public void setFcmToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }

    public String getFcmToken() {
        return pref.getString(KEY_FCM_TOKEN, null);
    }

    // ==================== Language Settings ====================

    public void setLanguage(String language) {
        editor.putString(Constants.PREF_LANGUAGE, language);
        editor.apply();
    }

    public String getLanguage() {
        return pref.getString(Constants.PREF_LANGUAGE, "vi");
    }

    // ==================== Theme Settings ====================

    public void setTheme(String theme) {
        editor.putString(Constants.PREF_THEME, theme);
        editor.apply();
    }

    public String getTheme() {
        return pref.getString(Constants.PREF_THEME, "light");
    }
}
