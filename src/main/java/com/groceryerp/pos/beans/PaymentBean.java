package com.groceryerp.pos.beans;
import java.io.Serializable;

/** JavaBean representing a payment associated with a sale. */
public class PaymentBean implements Serializable {
    private String paymentId;
    private String saleId;
    private String method;   // CASH, CARD, WALLET
    private double amountPaid;
    private double change;
    private String processedAt;

    public PaymentBean() {}

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
