package com.example.myapplication.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for formatting currency and dates
 */
public class FormatUtils {
    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(VIETNAM_LOCALE);
    private static final DecimalFormat decimalFormat = new DecimalFormat("#,###");
    
    /**
     * Format amount as Vietnamese currency
     */
    public static String formatCurrency(double amount) {
        return decimalFormat.format(amount) + " ₫";
    }

    /**
     * Format amount as Vietnamese currency without symbol
     */
    public static String formatNumber(double amount) {
        return decimalFormat.format(amount);
    }

    /**
     * Format amount with + or - sign for transaction display
     */
    public static String formatTransactionAmount(double amount, boolean isIncoming) {
        String prefix = isIncoming ? "+" : "-";
        return prefix + formatCurrency(Math.abs(amount));
    }

    /**
     * Parse currency string to double
     */
    public static double parseCurrency(String currencyString) {
        try {
            String cleanString = currencyString.replaceAll("[^\\d]", "");
            return Double.parseDouble(cleanString);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Format date for display
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, VIETNAM_LOCALE);
        return sdf.format(date);
    }

    /**
     * Format date and time for display
     */
    public static String formatDateTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_FULL, VIETNAM_LOCALE);
        return sdf.format(date);
    }

    /**
     * Format time only
     */
    public static String formatTime(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_TIME, VIETNAM_LOCALE);
        return sdf.format(date);
    }

    /**
     * Format relative time (e.g., "2 hours ago", "Yesterday")
     */
    public static String formatRelativeTime(Date date) {
        if (date == null) return "";
        
        long diff = System.currentTimeMillis() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days == 1) {
            return "Hôm qua";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            return formatDate(date);
        }
    }

    /**
     * Format account number with spaces for readability
     * e.g., "TDTU12345678" -> "TDTU 1234 5678"
     */
    public static String formatAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < accountNumber.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(accountNumber.charAt(i));
        }
        return formatted.toString();
    }

    /**
     * Mask account number for security
     * e.g., "TDTU12345678" -> "****5678"
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Format phone number
     * e.g., "0901234567" -> "090 123 4567"
     */
    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.length() < 10) {
            return phone;
        }
        return phone.substring(0, 3) + " " + phone.substring(3, 6) + " " + phone.substring(6);
    }

    /**
     * Mask phone number for privacy
     * e.g., "0901234567" -> "090***4567"
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 10) {
            return phone;
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }

    /**
     * Format interest rate for display
     */
    public static String formatInterestRate(double rate) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(rate) + "%/năm";
    }

    /**
     * Get greeting based on time of day
     */
    public static String getGreeting() {
        int hour = new Date().getHours();
        if (hour < 12) {
            return "Chào buổi sáng";
        } else if (hour < 18) {
            return "Chào buổi chiều";
        } else {
            return "Chào buổi tối";
        }
    }

    /**
     * Parse date from string
     */
    public static Date parseDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, VIETNAM_LOCALE);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }
}
