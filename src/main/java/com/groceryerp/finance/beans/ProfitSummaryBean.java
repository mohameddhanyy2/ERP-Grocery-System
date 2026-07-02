package com.groceryerp.finance.beans;

import java.io.Serializable;

/**
 * ProfitSummaryBean — pure in-memory DTO / value object for profit summaries.
 *
 * NOT a JPA @Entity: it maps to no table and had no nested DAO. It is computed
 * on demand from RevenueBean / ExpenseBean aggregates and returned to callers,
 * never persisted. It therefore stays a plain Serializable class with no
 * jakarta.persistence annotations.
 */
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
