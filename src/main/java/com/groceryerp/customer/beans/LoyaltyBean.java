package com.groceryerp.customer.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="loyalty")
/*
 * LoyaltyBean — Entity Bean mapped to the {@code loyalty} table.
 * One row per customer loyalty record. Bean type: @Entity.
 */
/** JavaBean representing loyalty points for a customer. */
public class LoyaltyBean implements Serializable {
    private String customerId;
    private int points;
    private String tier;

    public LoyaltyBean() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the loyalty table. */
    public static class DAO {

        /** Inserts or replaces a LoyaltyBean in the loyalty table. */
        public void save(LoyaltyBean loyalty) {
            String sql = "INSERT OR REPLACE INTO loyalty (customerId,points,tier) VALUES (?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, loyalty.getCustomerId());
                ps.setInt(2, loyalty.getPoints());
                ps.setString(3, loyalty.getTier());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save loyalty: " + e.getMessage());
            }
        }

        /** Finds a LoyaltyBean by customer ID, or returns null if not found. */
        public LoyaltyBean findByCustomerId(String customerId) {
            String sql = "SELECT customerId,points,tier FROM loyalty WHERE customerId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LoyaltyBean l = new LoyaltyBean();
                        l.setCustomerId(rs.getString("customerId"));
                        l.setPoints(rs.getInt("points"));
                        l.setTier(rs.getString("tier"));
                        return l;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find loyalty: " + e.getMessage());
            }
            return null;
        }

        /**
         * Adds points to a customer's loyalty record and recalculates their tier.
         * Tier rules: 0-499 = BRONZE, 500-1499 = SILVER, 1500+ = GOLD.
         */
        public void addPoints(String customerId, int points) {
            String selectSql = "SELECT points FROM loyalty WHERE customerId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
                selectPs.setString(1, customerId);
                try (ResultSet rs = selectPs.executeQuery()) {
                    if (rs.next()) {
                        int newPoints = rs.getInt("points") + points;
                        String tier = newPoints >= 1500 ? "GOLD" : newPoints >= 500 ? "SILVER" : "BRONZE";
                        String updateSql = "UPDATE loyalty SET points = ?, tier = ? WHERE customerId = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setInt(1, newPoints);
                            updatePs.setString(2, tier);
                            updatePs.setString(3, customerId);
                            updatePs.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to add loyalty points: " + e.getMessage());
            }
        }
    }
}

// conflicts resolved by: Omar Khalifa