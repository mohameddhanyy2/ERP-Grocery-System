package com.groceryerp.customer.beans;

import java.io.Serializable;
import java.time.LocalDate;

/** JavaBean representing a PurchaseHistory entry for a customer. */
public class PurchaseHistoryBean implements Serializable {
    private int customerId;
    private String saleId;
    private LocalDate date;
    private double amount;

    public PurchaseHistoryBean() {}

    public PurchaseHistoryBean(int customerId, String saleId, LocalDate date, double amount) {
        this.customerId = customerId;
        this.saleId = saleId;
        this.date = date;
        this.amount = amount;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "PurchaseHistoryBean{" +
                "customerId=" + customerId +
                ", saleId='" + saleId + '\'' +
                ", date=" + date +
                ", amount=" + amount +
                '}';
    }
}
