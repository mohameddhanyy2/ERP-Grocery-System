package com.groceryerp.pos.beans;
import java.io.Serializable;

/** JavaBean representing a completed sale transaction. */
public class SaleBean implements Serializable {
    private String saleId;
    private String storeId;
    private String customerId;
    private double totalAmount;
    private String paymentMethod;
    private String timestamp;
    private double discountRate;

    public SaleBean() {}

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
}
