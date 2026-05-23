package com.groceryerp.finance.beans;

// @Stateless (value object — computed on demand, not stored in a table, no DAO)
/**
 * ProfitSummaryBean — computed result object for profit summaries.
 * Not persisted. Bean type: @Stateless value object.
 */
import java.io.Serializable;

/** JavaBean representing a profit summary for a period. */
public class ProfitSummaryBean implements Serializable {
    private String storeId;
    private String period;
    private double grossRevenue;
    private double totalExpenses;
    private double netProfit;


    public ProfitSummaryBean() {}

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(double grossRevenue) { this.grossRevenue = grossRevenue; }

    public double getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }

    public double getNetProfit() { return netProfit; }
    public void setNetProfit(double netProfit) { this.netProfit = netProfit; }
}

// conflicts resolved by: Omar Khalifa