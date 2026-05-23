package com.groceryerp.customer.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="purchase_history")
/**
 * PurchaseHistoryBean — Entity Bean mapped to the {@code purchase_history} table.
 * One row per purchase history entry for a customer. Bean type: @Entity.
 */
public class PurchaseHistoryBean implements Serializable {

    // @Id
    /** Unique history record identifier. */
    private String historyId;
    /** ID of the customer. */
    private String customerId;
    /** ID of the sale this history record references. */
    private String saleId;
    /** ISO-8601 date of the purchase. */
    private String date;
    /** Amount spent in this purchase. */
    private double amount;

    /** No-argument constructor required by JavaBeans spec. */
    public PurchaseHistoryBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the purchase_history table. */
    public static class DAO {

        /** Persists a PurchaseHistoryBean to the purchase_history table. */
        public void save(PurchaseHistoryBean history) {
            String sql = "INSERT OR REPLACE INTO purchase_history (historyId,customerId,saleId,date,amount) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, history.getHistoryId());
                ps.setString(2, history.getCustomerId());
                ps.setString(3, history.getSaleId());
                ps.setString(4, history.getDate());
                ps.setDouble(5, history.getAmount());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save purchase history: " + e.getMessage());
            }
        }

        /** Returns all sale IDs from purchase history for a given customer. */
        public List<String> findIdsByCustomer(String customerId) {
            List<String> list = new ArrayList<>();
            String sql = "SELECT saleId FROM purchase_history WHERE customerId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { list.add(rs.getString("saleId")); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find purchase history: " + e.getMessage());
            }
            return list;
        }
    }
}
