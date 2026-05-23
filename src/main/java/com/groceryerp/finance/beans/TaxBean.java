package com.groceryerp.finance.beans;

import com.groceryerp.db.DatabaseManager;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="tax_records")
/*
 * TaxBean — Entity Bean mapped to the {@code tax_records} table.
 * One row per tax record per period. Bean type: @Entity.
 */
/** JavaBean representing tax calculation details. */
public class TaxBean implements Serializable {
    private String taxId;
    private String period;
    private double taxRate;
    private double taxableAmount;
    private double taxOwed;

    public TaxBean() {}


    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public double getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(double taxableAmount) { this.taxableAmount = taxableAmount; }

    public double getTaxOwed() { return taxOwed; }
    public void setTaxOwed(double taxOwed) { this.taxOwed = taxOwed; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the tax_records table. */
    public static class DAO {

        /** Persists a TaxBean to the tax_records table. */
        public void save(TaxBean tax) {
            String sql = "INSERT OR REPLACE INTO tax_records (taxId,period,taxRate,taxableAmount,taxOwed) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tax.getTaxId());
                ps.setString(2, tax.getPeriod());
                ps.setDouble(3, tax.getTaxRate());
                ps.setDouble(4, tax.getTaxableAmount());
                ps.setDouble(5, tax.getTaxOwed());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save tax record: " + e.getMessage());
            }
        }

        /** Finds a TaxBean by period, or returns null if not found. */
        public TaxBean findByPeriod(String period) {
            String sql = "SELECT taxId,period,taxRate,taxableAmount,taxOwed FROM tax_records WHERE period = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, period);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        TaxBean t = new TaxBean();
                        t.setTaxId(rs.getString("taxId"));
                        t.setPeriod(rs.getString("period"));
                        t.setTaxRate(rs.getDouble("taxRate"));
                        t.setTaxableAmount(rs.getDouble("taxableAmount"));
                        t.setTaxOwed(rs.getDouble("taxOwed"));
                        return t;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find tax record: " + e.getMessage());
            }
            return null;
        }
    }
}

// conflicts resolved by: Omar Khalifa