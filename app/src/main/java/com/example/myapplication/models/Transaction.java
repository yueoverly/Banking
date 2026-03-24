package com.example.myapplication.models;

import java.util.Date;
import java.util.UUID;

public class Transaction {

    public enum TransactionType {
        TRANSFER,
        DEPOSIT,
        WITHDRAWAL,
        BILL_PAYMENT,
        PHONE_TOPUP,
        LOAN_PAYMENT
    }

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private String id;
    private String fromAccountId;
    private String toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String recipientName;
    private double amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String referenceNumber;
    private Date createdAt;

    public Transaction() {
        this.status = TransactionStatus.PENDING;
        this.createdAt = new Date();
        this.referenceNumber = generateReferenceNumber();
    }

    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // Getters
    public String getId() { return id; }
    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public String getFromAccountNumber() { return fromAccountNumber; }
    public String getToAccountNumber() { return toAccountNumber; }
    public String getRecipientName() { return recipientName; }
    public double getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getReferenceNumber() { return referenceNumber; }
    public Date getCreatedAt() { return createdAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }
    public void setFromAccountNumber(String fromAccountNumber) { this.fromAccountNumber = fromAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setType(TransactionType type) { this.type = type; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
