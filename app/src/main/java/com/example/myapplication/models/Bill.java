package com.example.myapplication.models;

import java.io.Serializable;
import java.util.Date;

/**
 * Bill model for utility bill payments
 */
public class Bill implements Serializable {
    private String id;
    private String userId;
    private BillType billType;
    private String providerCode;
    private String providerName;
    private String customerCode;
    private String customerName;
    private double amount;
    private String currency;
    private BillStatus status;
    private Date dueDate;
    private Date paidDate;
    private String transactionId;
    private String period; // e.g., "Tháng 12/2024"
    private Date createdAt;
    private Date updatedAt;

    public enum BillType {
        ELECTRICITY("Điện", "ic_electricity"),
        WATER("Nước", "ic_water"),
        INTERNET("Internet", "ic_internet"),
        PHONE("Điện thoại", "ic_phone"),
        TV("Truyền hình", "ic_tv"),
        INSURANCE("Bảo hiểm", "ic_insurance");

        private final String displayName;
        private final String iconName;

        BillType(String displayName, String iconName) {
            this.displayName = displayName;
            this.iconName = iconName;
        }

        public String getDisplayName() { return displayName; }
        public String getIconName() { return iconName; }
    }

    public enum BillStatus {
        UNPAID("Chưa thanh toán"),
        PAID("Đã thanh toán"),
        OVERDUE("Quá hạn"),
        CANCELLED("Đã hủy");

        private final String displayName;

        BillStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    public Bill() {
        this.currency = "VND";
        this.status = BillStatus.UNPAID;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public BillType getBillType() { return billType; }
    public void setBillType(BillType billType) { this.billType = billType; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public Date getPaidDate() { return paidDate; }
    public void setPaidDate(Date paidDate) { this.paidDate = paidDate; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOverdue() {
        return status == BillStatus.UNPAID && dueDate != null && dueDate.before(new Date());
    }
}
