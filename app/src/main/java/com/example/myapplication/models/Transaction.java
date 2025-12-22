package com.example.myapplication.models;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Transaction model representing all banking transactions
 */
public class Transaction implements Serializable {
    private String id;
    private String transactionCode;
    private String fromAccountId;
    private String toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String fromBankName;
    private String toBankName;
    private double amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private double fee;
    private Date createdAt;
    private Date completedAt;
    private String otpCode;
    private boolean isOtpVerified;
    private boolean isFaceVerified;
    private String recipientName;

    public enum TransactionType {
        TRANSFER_INTERNAL("Chuyển khoản nội bộ"),
        TRANSFER_EXTERNAL("Chuyển khoản liên ngân hàng"),
        DEPOSIT("Nạp tiền"),
        WITHDRAWAL("Rút tiền"),
        BILL_PAYMENT("Thanh toán hóa đơn"),
        PHONE_TOPUP("Nạp tiền điện thoại"),
        TICKET_PURCHASE("Mua vé"),
        LOAN_PAYMENT("Trả nợ vay"),
        INTEREST_PAYMENT("Thanh toán lãi"),
        QR_PAYMENT("Thanh toán QR");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TransactionStatus {
        PENDING("Đang chờ"),
        OTP_REQUIRED("Cần xác thực OTP"),
        FACE_REQUIRED("Cần xác thực khuôn mặt"),
        PROCESSING("Đang xử lý"),
        COMPLETED("Hoàn thành"),
        FAILED("Thất bại"),
        CANCELLED("Đã hủy");

        private final String displayName;

        TransactionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.transactionCode = generateTransactionCode();
        this.createdAt = new Date();
        this.currency = "VND";
        this.status = TransactionStatus.PENDING;
        this.fee = 0;
        this.isOtpVerified = false;
        this.isFaceVerified = false;
    }

    public Transaction(String fromAccountId, String toAccountId, double amount, TransactionType type) {
        this();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.type = type;
    }

    private String generateTransactionCode() {
        long timestamp = System.currentTimeMillis();
        return "TXN" + timestamp + String.format("%04d", (int)(Math.random() * 10000));
    }

    // Check if transaction requires 2FA (high value transactions)
    public boolean requires2FA() {
        // Transactions above 10,000,000 VND require 2FA
        return amount >= 10000000;
    }

    // Check if transaction requires face verification (very high value)
    public boolean requiresFaceVerification() {
        // Transactions above 50,000,000 VND require face verification
        return amount >= 50000000;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }

    public String getToAccountId() { return toAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }

    public String getFromAccountNumber() { return fromAccountNumber; }
    public void setFromAccountNumber(String fromAccountNumber) { this.fromAccountNumber = fromAccountNumber; }

    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }

    public String getFromBankName() { return fromBankName; }
    public void setFromBankName(String fromBankName) { this.fromBankName = fromBankName; }

    public String getToBankName() { return toBankName; }
    public void setToBankName(String toBankName) { this.toBankName = toBankName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public boolean isOtpVerified() { return isOtpVerified; }
    public void setOtpVerified(boolean otpVerified) { isOtpVerified = otpVerified; }

    public boolean isFaceVerified() { return isFaceVerified; }
    public void setFaceVerified(boolean faceVerified) { isFaceVerified = faceVerified; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public double getTotalAmount() {
        return amount + fee;
    }

    public boolean isIncoming(String accountId) {
        return toAccountId != null && toAccountId.equals(accountId);
    }

    public boolean isOutgoing(String accountId) {
        return fromAccountId != null && fromAccountId.equals(accountId);
    }
}
