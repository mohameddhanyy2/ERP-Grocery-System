package com.groceryerp.pos.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * PaymentBean — JPA entity mapped to the {@code payments} table.
 * One row per payment record for a sale.
 *
 * The nested DAO is gone — persistence is handled by
 * {@link com.groceryerp.pos.POSRepository}. Bean type: @Entity.
 */
@Entity
@Table(name = "payments")
public class PaymentBean implements Serializable {

    @Id
    @Column(name = "paymentId")
    /** Unique payment identifier, e.g. "PAY-1234567890". */
    private String paymentId;
    /** ID of the sale this payment is for. */
    @Column(name = "saleId")
    private String saleId;
    /** Payment method: CASH, CARD, or WALLET. */
    @Column(name = "method")
    private String method;
    /** Amount the customer paid. */
    @Column(name = "amountPaid")
    private double amountPaid;
    /** Change returned to customer (amountPaid - grandTotal). */
    @Column(name = "change")
    private double change;
    /** ISO-8601 timestamp of when the payment was processed. */
    @Column(name = "processedAt")
    private String processedAt;

    /** No-argument constructor required by JavaBeans spec. */
    public PaymentBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }
}

// reviewed by: Omar Khalifa
