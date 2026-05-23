package com.groceryerp.reporting.beans;
import java.io.Serializable;

// @Stateless (value object — computed on demand, not stored in a table, no DAO)
/**
 * SalesSummaryBean — computed result object for sales summaries.
 * Not persisted. Bean type: @Stateless value object.
 */
public class SalesSummaryBean implements Serializable {
    private String period;
    private double grossRevenue;
    private int transactionCount;
    private String topSellingProduct;
    private String storeId;

    public SalesSummaryBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public double getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(double grossRevenue) { this.grossRevenue = grossRevenue; }
    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    public String getTopSellingProduct() { return topSellingProduct; }
    public void setTopSellingProduct(String topSellingProduct) { this.topSellingProduct = topSellingProduct; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
}
