package com.example.myapplication.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount) + " đ";
    }

    public static String formatCurrencyNoSymbol(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }

    public static String formatDateTime(Date date) {
        if (date == null) return "";
        return DATETIME_FORMAT.format(date);
    }

    public static String formatTime(Date date) {
        if (date == null) return "";
        return TIME_FORMAT.format(date);
    }

    public static String formatAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() != 16) return accountNumber;
        return accountNumber.substring(0, 4) + " " + 
               accountNumber.substring(4, 8) + " " + 
               accountNumber.substring(8, 12) + " " + 
               accountNumber.substring(12, 16);
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return accountNumber;
        return "**** **** **** " + accountNumber.substring(accountNumber.length() - 4);
    }

    public static String formatInterestRate(double rate) {
        return String.format(Locale.getDefault(), "%.2f%%", rate);
    }

    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.length() != 10) return phone;
        return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
    }
}
