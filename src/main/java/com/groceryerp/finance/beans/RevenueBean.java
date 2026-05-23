package com.groceryerp.finance.beans;

import com.groceryerp.db.DatabaseManager;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="revenue")
/*
 * RevenueBean — Entity Bean mapped to the {@code revenue} table.
 * One row per revenue record per store per period. Bean type: @Entity.
 */
/** JavaBean representing revenue for a period/store. */
public class RevenueBean implements Serializable {
    private String revenueId;
    private String period;
    private double grossRevenue;
    private String storeId;

    public RevenueBean() {}

    public String getRevenueId() { return revenueId; }
    public void setRevenueId(String revenueId) { this.revenueId = revenueId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public double getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(double grossRevenue) { this.grossRevenue = grossRevenue; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the revenue table. */
    public static class DAO {

        /** Persists a RevenueBean to the revenue table. */
        public void save(RevenueBean revenue) {
            String sql = "INSERT OR REPLACE INTO revenue (revenueId,period,storeId,grossRevenue) VALUES (?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, revenue.getRevenueId());
                ps.setString(2, revenue.getPeriod());
                ps.setString(3, revenue.getStoreId());
                ps.setDouble(4, revenue.getGrossRevenue());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save revenue: " + e.getMessage());
            }
        }

        /** Returns the sum of grossRevenue for a given period. */
        public double sumByPeriod(String period) {
            String sql = "SELECT SUM(grossRevenue) FROM revenue WHERE period = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, period);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return rs.getDouble(1); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to sum revenue: " + e.getMessage());
            }
            return 0.0;
        }
    }
}

// conflicts resolved by: Omar Khalifa