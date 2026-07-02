package com.groceryerp.finance.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * RevenueBean — JPA entity mapped to the {@code revenue} table.
 * One row per revenue record per store per period.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT SUM). The DAO is gone — persistence is
 * now handled by {@link com.groceryerp.finance.FinanceRepository} via the
 * container-managed EntityManager.
 */
@Entity
@Table(name = "revenue")
public class RevenueBean implements Serializable {

    @Id
    @Column(name = "revenueId")
    private String revenueId;

    @Column(name = "period")
    private String period;

    @Column(name = "grossRevenue")
    private double grossRevenue;

    @Column(name = "storeId")
    private String storeId;

    @Column(name = "saleId")
    private String saleId;

    public RevenueBean() {}

    public String getRevenueId() { return revenueId; }
    public void setRevenueId(String revenueId) { this.revenueId = revenueId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public double getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(double grossRevenue) { this.grossRevenue = grossRevenue; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }
}

// conflicts resolved by: Omar Khalifa
