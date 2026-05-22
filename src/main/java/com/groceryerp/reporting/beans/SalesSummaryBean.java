package com.groceryerp.reporting.beans;
import java.io.Serializable;

/** JavaBean carrying a sales summary for a given period. */
public class SalesSummaryBean implements Serializable {
    private String period;
    private double grossRevenue;
    private int transactionCount;
    private String topSellingProduct;
    private String storeId;

    public SalesSummaryBean() {}

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
