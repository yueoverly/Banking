package com.example.myapplication.models;

import java.util.Date;
import java.util.Random;

public class Account {

    public enum AccountType {
        CHECKING,
        SAVING,
        MORTGAGE
    }

    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        FROZEN,
        CLOSED
    }

    public enum PaymentFrequency {
        MONTHLY,      // Hàng tháng
        BI_WEEKLY     // Mỗi 2 tuần
    }

    private String id;
    private String userId;
    private String accountNumber;
    private AccountType accountType;
    private AccountStatus status;
    private double balance;
    private double interestRate;          // Lãi suất (%)
    private double loanAmount;            // Số tiền vay (Mortgage)
    private int loanTermMonths;           // Kỳ hạn vay (tháng)
    private double remainingDebt;         // Dư nợ còn lại (Mortgage)
    private PaymentFrequency paymentFrequency; // Tần suất trả nợ
    private Date createdAt;
    private Date updatedAt;

    public Account() {
        this.status = AccountStatus.ACTIVE;
        this.balance = 0;
        this.paymentFrequency = PaymentFrequency.MONTHLY;
    }

    public Account(String userId, AccountType accountType) {
        this.userId = userId;
        this.accountType = accountType;
        this.accountNumber = generateAccountNumber();
        this.status = AccountStatus.ACTIVE;
        this.balance = 0;
        this.paymentFrequency = PaymentFrequency.MONTHLY;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        
        // Set default interest rates
        if (accountType == AccountType.SAVING) {
            this.interestRate = 5.5; // 5.5% năm
        } else if (accountType == AccountType.MORTGAGE) {
            this.interestRate = 8.0; // 8% năm
        }
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public String getFormattedAccountNumber() {
        if (accountNumber == null || accountNumber.length() != 16) return accountNumber;
        return accountNumber.substring(0, 4) + " " + 
               accountNumber.substring(4, 8) + " " + 
               accountNumber.substring(8, 12) + " " + 
               accountNumber.substring(12, 16);
    }

    /**
     * Tính lợi nhuận hàng tháng cho tài khoản tiết kiệm
     */
    public double getMonthlyProfit() {
        if (accountType == AccountType.SAVING && interestRate > 0 && balance > 0) {
            return balance * (interestRate / 100) / 12;
        }
        return 0;
    }

    /**
     * Tính lợi nhuận hàng năm cho tài khoản tiết kiệm
     */
    public double getYearlyProfit() {
        if (accountType == AccountType.SAVING && interestRate > 0 && balance > 0) {
            return balance * (interestRate / 100);
        }
        return 0;
    }

    /**
     * Tính số tiền cần trả mỗi tháng cho tài khoản vay
     */
    public double getMonthlyPayment() {
        if (accountType == AccountType.MORTGAGE && loanAmount > 0 && loanTermMonths > 0 && interestRate > 0) {
            double monthlyRate = interestRate / 100 / 12;
            return loanAmount * monthlyRate * Math.pow(1 + monthlyRate, loanTermMonths) 
                   / (Math.pow(1 + monthlyRate, loanTermMonths) - 1);
        }
        return 0;
    }

    /**
     * Tính số tiền cần trả mỗi 2 tuần cho tài khoản vay
     */
    public double getBiWeeklyPayment() {
        return getMonthlyPayment() / 2;
    }

    /**
     * Lấy số tiền cần trả theo tần suất đã chọn
     */
    public double getPaymentAmount() {
        if (paymentFrequency == PaymentFrequency.BI_WEEKLY) {
            return getBiWeeklyPayment();
        }
        return getMonthlyPayment();
    }

    /**
     * Lấy tên tần suất trả nợ
     */
    public String getPaymentFrequencyName() {
        if (paymentFrequency == PaymentFrequency.BI_WEEKLY) {
            return "Mỗi 2 tuần";
        }
        return "Hàng tháng";
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getAccountNumber() { return accountNumber; }
    public AccountType getAccountType() { return accountType; }
    public AccountStatus getStatus() { return status; }
    public double getBalance() { return balance; }
    public double getInterestRate() { return interestRate; }
    public double getLoanAmount() { return loanAmount; }
    public int getLoanTermMonths() { return loanTermMonths; }
    public double getRemainingDebt() { return remainingDebt; }
    public PaymentFrequency getPaymentFrequency() { return paymentFrequency; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public void setLoanAmount(double loanAmount) { this.loanAmount = loanAmount; }
    public void setLoanTermMonths(int loanTermMonths) { this.loanTermMonths = loanTermMonths; }
    public void setRemainingDebt(double remainingDebt) { this.remainingDebt = remainingDebt; }
    public void setPaymentFrequency(PaymentFrequency paymentFrequency) { this.paymentFrequency = paymentFrequency; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
