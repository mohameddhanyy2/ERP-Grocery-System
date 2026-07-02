package com.groceryerp.pos.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * ReceiptBean — JPA entity mapped to the {@code receipts} table.
 * One row per receipt issued after a completed sale.
 *
 * The nested DAO is gone — persistence is handled by
 * {@link com.groceryerp.pos.POSRepository}. Bean type: @Entity.
 */
@Entity
@Table(name = "receipts")
public class ReceiptBean implements Serializable {

    @Id
    @Column(name = "receiptId")
    /** Unique receipt identifier, e.g. "RECEIPT-1234567890". */
    private String receiptId;
    /** ID of the sale this receipt belongs to. */
    @Column(name = "saleId")
    private String saleId;
    /** ID of the store where the sale took place. */
    @Column(name = "storeId")
    private String storeId;
    /** Total sale amount before tax. */
    @Column(name = "totalAmount")
    private double totalAmount;
    /** Tax amount — 14% of totalAmount. */
    @Column(name = "taxAmount")
    private double taxAmount;
    /** Grand total — totalAmount + taxAmount. */
    @Column(name = "grandTotal")
    private double grandTotal;
    /** ISO-8601 timestamp of when the receipt was issued. */
    @Column(name = "issuedAt")
    private String issuedAt;

    /** No-argument constructor required by JavaBeans spec. */
    public ReceiptBean() { /* no-arg constructor required by JavaBeans spec */ }

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

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }
}

// reviewed by: Omar Khalifa
