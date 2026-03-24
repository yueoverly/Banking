package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.models.User;

public class SessionManager {

    private static final String PREF_NAME = "TDTBankSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_BIOMETRIC_ENABLED = "biometricEnabled";

    private static SessionManager instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    private SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void createLoginSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_NAME, user.getFullName());
        editor.putString(KEY_USER_PHONE, user.getPhone());
        editor.putString(KEY_USER_TYPE, user.getUserType().name());
        editor.apply();
    }

    public User getUser() {
        if (!isLoggedIn()) return null;

        User user = new User();
        user.setId(pref.getString(KEY_USER_ID, null));
        user.setEmail(pref.getString(KEY_USER_EMAIL, null));
        user.setFullName(pref.getString(KEY_USER_NAME, null));
        user.setPhone(pref.getString(KEY_USER_PHONE, null));
        
        String userTypeStr = pref.getString(KEY_USER_TYPE, User.UserType.CUSTOMER.name());
        user.setUserType(User.UserType.valueOf(userTypeStr));
        
        return user;
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public User.UserType getUserType() {
        String type = pref.getString(KEY_USER_TYPE, User.UserType.CUSTOMER.name());
        return User.UserType.valueOf(type);
    }

    public void setBiometricEnabled(boolean enabled) {
        editor.putBoolean(KEY_BIOMETRIC_ENABLED, enabled);
        editor.apply();
    }

    public boolean isBiometricEnabled() {
        return pref.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void logout() {
        editor.clear();
        editor.apply();
        FirebaseHelper.getInstance().signOut();
    }
}
