package com.groceryerp.customer.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * PurchaseHistoryBean — JPA entity mapped to the {@code purchase_history} table.
 *
 * Was: a JavaBean with a nested DAO (INSERT OR REPLACE + a SELECT saleId query).
 * Persistence now flows through {@link com.groceryerp.customer.CustomerRepository}.
 */
@Entity
@Table(name = "purchase_history")
public class PurchaseHistoryBean implements Serializable {

    @Id
    @Column(name = "historyId")
    private String historyId;

    @Column(name = "customerId")
    private String customerId;

    @Column(name = "saleId")
    private String saleId;

    @Column(name = "date")
    private String date;

    @Column(name = "amount")
    private double amount;

    public PurchaseHistoryBean() {}

    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
