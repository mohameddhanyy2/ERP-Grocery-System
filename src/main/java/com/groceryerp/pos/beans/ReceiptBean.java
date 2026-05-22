package com.groceryerp.pos.beans;
import java.io.Serializable;

/** JavaBean representing a receipt issued after a sale. */
public class ReceiptBean implements Serializable {
    private String receiptId;
    private String saleId;
    private String storeId;
    private double totalAmount;
    private double taxAmount;
    private String issuedAt;

    public ReceiptBean() {}

    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }
    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }
}
