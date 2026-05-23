package com.groceryerp.finance.beans;

import java.io.Serializable;

/** JavaBean representing revenue for a period/store. */
public class RevenueBean implements Serializable {
    private String period;
    private double grossRevenue;
    private String storeId;

    public RevenueBean() {}

    public RevenueBean(String period, double grossRevenue, String storeId) {
        this.period = period;
        this.grossRevenue = grossRevenue;
        this.storeId = storeId;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(double grossRevenue) { this.grossRevenue = grossRevenue; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    @Override
    public String toString() {
        return "RevenueBean{" +
                "period='" + period + '\'' +
                ", grossRevenue=" + grossRevenue +
                ", storeId='" + storeId + '\'' +
                '}';
    }
}
