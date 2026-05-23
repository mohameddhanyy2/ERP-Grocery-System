package com.groceryerp.finance.beans;

import java.io.Serializable;

/** JavaBean representing a profit summary for a period. */
public class ProfitSummaryBean implements Serializable {
    private String period;
    private double grossRevenue;
    private double totalExpenses;
    private double netProfit;

    public ProfitSummaryBean() {}

    public ProfitSummaryBean(String period, double grossRevenue, double totalExpenses, double netProfit) {
        this.period = period;
        this.grossRevenue = grossRevenue;
        this.totalExpenses = totalExpenses;
        this.netProfit = netProfit;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(double grossRevenue) { this.grossRevenue = grossRevenue; }

    public double getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(double totalExpenses) { this.totalExpenses = totalExpenses; }

    public double getNetProfit() { return netProfit; }
    public void setNetProfit(double netProfit) { this.netProfit = netProfit; }

    @Override
    public String toString() {
        return "ProfitSummaryBean{" +
                "period='" + period + '\'' +
                ", grossRevenue=" + grossRevenue +
                ", totalExpenses=" + totalExpenses +
                ", netProfit=" + netProfit +
                '}';
    }
}
