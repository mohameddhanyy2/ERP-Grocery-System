package com.groceryerp.pos.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * SaleBean — JPA entity mapped to the {@code sales} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT). The DAO is gone — persistence is now
 * handled by {@link com.groceryerp.pos.POSRepository} via the container-managed
 * EntityManager. The annotations below ARE the table mapping.
 *
 * Bean type: @Entity — one row in the sales table.
 */
@Entity
@Table(name = "sales")
public class SaleBean implements Serializable {

    @Id
    @Column(name = "saleId")
    /** Unique sale identifier, e.g. "SALE-1234567890". */
    private String saleId;
    /** ID of the store where the sale occurred. */
    @Column(name = "storeId")
    private String storeId;
    /** ID of the customer who made the purchase. */
    @Column(name = "customerId")
    private String customerId;
    /** Total monetary amount after discounts. */
    @Column(name = "totalAmount")
    private double totalAmount;
    /** Payment method: CASH, CARD, or WALLET. */
    @Column(name = "paymentMethod")
    private String paymentMethod;
    /** ISO-8601 timestamp of when the sale was processed. */
    @Column(name = "timestamp")
    private String timestamp;
    /** Discount rate applied (0.0 to 1.0). */
    @Column(name = "discountRate")
    private double discountRate;
    /** Line items included in this sale. Not a persisted column — populated in memory. */
    @Transient
    private List<SaleItemBean> items;

    /** No-argument constructor required by JavaBeans spec. */
    public SaleBean() {
        this.items = new ArrayList<>();
    }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getDiscountRate() { return discountRate; }
    public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }

    public List<SaleItemBean> getItems() { return items; }
    public void setItems(List<SaleItemBean> items) { this.items = items; }
}

// reviewed by: Omar Khalifa
