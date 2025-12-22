package com.example.myapplication.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Account model representing different types of bank accounts
 * Types: CHECKING, SAVING, MORTGAGE
 */
public class Account implements Serializable {
    private String id;
    private String accountNumber;
    private String userId;
    private AccountType accountType;
    private double balance;
    private String currency;
    private AccountStatus status;
    private Date createdAt;
    private Date updatedAt;
    
    // For Saving Account
    private double interestRate;
    private double monthlyProfit;
    private int termMonths;
    private Date maturityDate;
    
    // For Mortgage Account
    private double loanAmount;
    private double monthlyPayment;
    private double remainingBalance;
    private int totalPayments;
    private int completedPayments;
    private double mortgageInterestRate;
    private PaymentFrequency paymentFrequency;

    public enum AccountType {
        CHECKING("Tài khoản thanh toán"),
        SAVING("Tài khoản tiết kiệm"),
        MORTGAGE("Tài khoản vay");

        private final String displayName;

        AccountType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AccountStatus {
        ACTIVE("Hoạt động"),
        INACTIVE("Không hoạt động"),
        FROZEN("Đóng băng"),
        CLOSED("Đã đóng");

        private final String displayName;

        AccountStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum PaymentFrequency {
        MONTHLY("Hàng tháng"),
        BI_WEEKLY("Hai tuần một lần");

        private final String displayName;

        PaymentFrequency(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Account() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.currency = "VND";
        this.status = AccountStatus.ACTIVE;
    }

    public Account(String userId, AccountType accountType) {
        this();
        this.userId = userId;
        this.accountType = accountType;
        this.accountNumber = generateAccountNumber();
    }

    private String generateAccountNumber() {
        // Generate 12-digit account number starting with bank code
        long timestamp = System.currentTimeMillis();
        return "TDTU" + String.valueOf(timestamp).substring(5);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { 
        this.balance = balance;
        this.updatedAt = new Date();
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Saving Account
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMonthlyProfit() { return monthlyProfit; }
    public void setMonthlyProfit(double monthlyProfit) { this.monthlyProfit = monthlyProfit; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public Date getMaturityDate() { return maturityDate; }
    public void setMaturityDate(Date maturityDate) { this.maturityDate = maturityDate; }

    // Mortgage Account
    public double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }

    public double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(double monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }

    public int getTotalPayments() { return totalPayments; }
    public void setTotalPayments(int totalPayments) { this.totalPayments = totalPayments; }

    public int getCompletedPayments() { return completedPayments; }
    public void setCompletedPayments(int completedPayments) { this.completedPayments = completedPayments; }

    public double getMortgageInterestRate() { return mortgageInterestRate; }
    public void setMortgageInterestRate(double mortgageInterestRate) { this.mortgageInterestRate = mortgageInterestRate; }

    public PaymentFrequency getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(PaymentFrequency paymentFrequency) { this.paymentFrequency = paymentFrequency; }

    // Helper methods
    public void calculateMonthlyProfit() {
        if (accountType == AccountType.SAVING && interestRate > 0) {
            this.monthlyProfit = balance * (interestRate / 100) / 12;
        }
    }

    public void calculateMortgagePayment() {
        if (accountType == AccountType.MORTGAGE && loanAmount > 0 && mortgageInterestRate > 0 && totalPayments > 0) {
            double monthlyRate = mortgageInterestRate / 100 / 12;
            this.monthlyPayment = loanAmount * (monthlyRate * Math.pow(1 + monthlyRate, totalPayments)) 
                                / (Math.pow(1 + monthlyRate, totalPayments) - 1);
        }
    }

    public boolean canWithdraw(double amount) {
        return status == AccountStatus.ACTIVE && balance >= amount && amount > 0;
    }

    public boolean canDeposit(double amount) {
        return status == AccountStatus.ACTIVE && amount > 0;
    }
}
