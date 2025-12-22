package com.example.myapplication.utils;

/**
 * Application constants
 */
public class Constants {
    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_ACCOUNTS = "accounts";
    public static final String COLLECTION_TRANSACTIONS = "transactions";
    public static final String COLLECTION_BILLS = "bills";
    public static final String COLLECTION_BRANCHES = "bank_branches";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_OTP = "otp_codes";

    // Shared Preferences
    public static final String PREF_NAME = "BankingAppPrefs";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_TYPE = "user_type";
    public static final String PREF_IS_LOGGED_IN = "is_logged_in";
    public static final String PREF_BIOMETRIC_ENABLED = "biometric_enabled";
    public static final String PREF_PIN_ENABLED = "pin_enabled";
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_THEME = "theme";

    // Intent Extras
    public static final String EXTRA_USER = "extra_user";
    public static final String EXTRA_ACCOUNT = "extra_account";
    public static final String EXTRA_TRANSACTION = "extra_transaction";
    public static final String EXTRA_ACCOUNT_ID = "extra_account_id";
    public static final String EXTRA_TRANSACTION_ID = "extra_transaction_id";
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_BILL_TYPE = "extra_bill_type";
    public static final String EXTRA_BRANCH = "extra_branch";

    // Transaction Limits
    public static final double TRANSACTION_LIMIT_DAILY = 500000000; // 500 million VND
    public static final double TRANSACTION_LIMIT_SINGLE = 200000000; // 200 million VND
    public static final double OTP_REQUIRED_AMOUNT = 10000000; // 10 million VND
    public static final double FACE_REQUIRED_AMOUNT = 50000000; // 50 million VND

    // OTP
    public static final int OTP_LENGTH = 6;
    public static final int OTP_EXPIRY_MINUTES = 5;

    // Interest Rates (default values)
    public static final double DEFAULT_SAVING_RATE_3M = 3.5; // 3.5% per year
    public static final double DEFAULT_SAVING_RATE_6M = 4.5;
    public static final double DEFAULT_SAVING_RATE_12M = 5.5;
    public static final double DEFAULT_MORTGAGE_RATE = 8.5;

    // Bank Information
    public static final String BANK_NAME = "TDTU Bank";
    public static final String BANK_CODE = "TDTU";
    public static final String BANK_SWIFT = "TDTUVNVX";

    // Phone TopUp Providers
    public static final String[] PHONE_PROVIDERS = {"Viettel", "Vinaphone", "Mobifone", "Vietnamobile", "Gmobile"};
    public static final int[] TOPUP_AMOUNTS = {10000, 20000, 30000, 50000, 100000, 200000, 300000, 500000};

    // Bill Providers
    public static final String[][] ELECTRICITY_PROVIDERS = {
            {"EVN_HCM", "EVN Hồ Chí Minh"},
            {"EVN_HN", "EVN Hà Nội"},
            {"EVN_DN", "EVN Đà Nẵng"}
    };

    public static final String[][] WATER_PROVIDERS = {
            {"SAWACO", "Cấp nước Sài Gòn"},
            {"HNWATER", "Cấp nước Hà Nội"}
    };

    // Date Formats
    public static final String DATE_FORMAT_DISPLAY = "dd/MM/yyyy";
    public static final String DATE_FORMAT_FULL = "dd/MM/yyyy HH:mm:ss";
    public static final String DATE_FORMAT_TIME = "HH:mm";

    // Validation
    public static final int MIN_PIN_LENGTH = 6;
    public static final int MAX_PIN_LENGTH = 6;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String PHONE_REGEX = "^(0|\\+84)[0-9]{9}$";
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String ID_CARD_REGEX = "^[0-9]{9,12}$";

    // Notification Channels
    public static final String NOTIFICATION_CHANNEL_TRANSACTION = "transaction_channel";
    public static final String NOTIFICATION_CHANNEL_PROMOTION = "promotion_channel";
    public static final String NOTIFICATION_CHANNEL_SECURITY = "security_channel";

    // Request Codes
    public static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final int REQUEST_LOCATION_PERMISSION = 101;
    public static final int REQUEST_STORAGE_PERMISSION = 102;
    public static final int REQUEST_BIOMETRIC = 103;
    public static final int REQUEST_QR_SCAN = 104;
    public static final int REQUEST_FACE_VERIFY = 105;
    public static final int REQUEST_IMAGE_CAPTURE = 106;
}
