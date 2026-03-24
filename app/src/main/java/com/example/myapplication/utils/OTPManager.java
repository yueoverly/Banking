package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.util.Random;

/**
 * OTP Manager - Quản lý tạo và xác thực OTP qua Email
 */
public class OTPManager {
    private static final String PREF_NAME = "otp_prefs";
    private static final String KEY_OTP = "current_otp";
    private static final String KEY_OTP_TIME = "otp_time";
    private static final String KEY_OTP_EMAIL = "otp_email";
    private static final long OTP_VALIDITY_MS = 5 * 60 * 1000; // 5 phút

    private static OTPManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    public interface OTPCallback {
        void onOTPSent(String email);
        void onError(String error);
    }

    private OTPManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized OTPManager getInstance(Context context) {
        if (instance == null) {
            instance = new OTPManager(context);
        }
        return instance;
    }

    /**
     * Tạo OTP 6 số ngẫu nhiên
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        String otpString = String.valueOf(otp);

        prefs.edit()
                .putString(KEY_OTP, otpString)
                .putLong(KEY_OTP_TIME, System.currentTimeMillis())
                .apply();

        return otpString;
    }

    /**
     * Gửi OTP qua Email
     */
    public void sendOTPEmail(String email, OTPCallback callback) {
        String otp = generateOTP();
        
        // Lưu email để hiển thị
        prefs.edit().putString(KEY_OTP_EMAIL, email).apply();

        // Kiểm tra email configuration
        if (!EmailSender.isConfigured()) {
            // Fallback: Hiển thị OTP qua Toast (demo mode)
            Toast.makeText(context, 
                "📧 [DEMO MODE]\nMã OTP: " + otp + "\n\n⚠️ Cần cấu hình EmailSender để gửi email thật", 
                Toast.LENGTH_LONG).show();
            
            if (callback != null) {
                callback.onOTPSent(email);
            }
            return;
        }

        // Gửi email thật
        EmailSender.sendOTPEmail(email, otp, new EmailSender.EmailCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(context, "✅ Đã gửi mã OTP đến " + maskEmail(email), Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onOTPSent(email);
                }
            }

            @Override
            public void onError(String error) {
                // Fallback to Toast on error
                Toast.makeText(context, 
                    "⚠️ Không gửi được email\nMã OTP: " + otp, 
                    Toast.LENGTH_LONG).show();
                
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Gửi OTP (backward compatible - uses email from session)
     */
    public void sendOTP(String phoneOrEmail) {
        // Get email from SessionManager
        String email = SessionManager.getInstance(context).getUserEmail();
        if (email != null && !email.isEmpty()) {
            sendOTPEmail(email, null);
        } else {
            // Fallback
            String otp = generateOTP();
            Toast.makeText(context, 
                "📱 Mã OTP: " + otp + "\n(Demo - Chưa có email)", 
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Xác thực OTP
     */
    public boolean verifyOTP(String inputOTP) {
        String savedOTP = prefs.getString(KEY_OTP, "");
        long otpTime = prefs.getLong(KEY_OTP_TIME, 0);

        if (System.currentTimeMillis() - otpTime > OTP_VALIDITY_MS) {
            clearOTP();
            return false;
        }

        if (savedOTP.equals(inputOTP)) {
            clearOTP();
            return true;
        }

        return false;
    }

    /**
     * Xóa OTP đã lưu
     */
    public void clearOTP() {
        prefs.edit()
                .remove(KEY_OTP)
                .remove(KEY_OTP_TIME)
                .remove(KEY_OTP_EMAIL)
                .apply();
    }

    /**
     * Lấy OTP hiện tại (chỉ dùng cho debug)
     */
    public String getCurrentOTP() {
        return prefs.getString(KEY_OTP, "");
    }

    /**
     * Lấy email đã gửi OTP
     */
    public String getOTPEmail() {
        return prefs.getString(KEY_OTP_EMAIL, "");
    }

    /**
     * Kiểm tra OTP còn hiệu lực không
     */
    public boolean isOTPValid() {
        long otpTime = prefs.getLong(KEY_OTP_TIME, 0);
        return System.currentTimeMillis() - otpTime <= OTP_VALIDITY_MS;
    }

    /**
     * Lấy thời gian còn lại của OTP (giây)
     */
    public int getRemainingSeconds() {
        long otpTime = prefs.getLong(KEY_OTP_TIME, 0);
        long elapsed = System.currentTimeMillis() - otpTime;
        long remaining = OTP_VALIDITY_MS - elapsed;
        return Math.max(0, (int) (remaining / 1000));
    }

    /**
     * Mask email để hiển thị an toàn
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        
        return name.substring(0, 2) + "***@" + domain;
    }
}
