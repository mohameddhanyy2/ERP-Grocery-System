package com.groceryerp.pos.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="discounts")
/**
 * DiscountBean — Entity Bean mapped to the {@code discounts} table.
 * One row per discount applied to a sale. Bean type: @Entity.
 */
public class DiscountBean implements Serializable {

    // @Id
    /** Unique discount identifier, e.g. "DISC-1234567890". */
    private String discountId;
    /** ID of the sale this discount applies to. */
    private String saleId;
    /** Discount type: PERCENTAGE or FIXED. */
    private String discountType;
    /** Value of the discount (percentage 0-100 or fixed amount). */
    private double discountValue;
    /** Human-readable description of the discount. */
    private String description;

    /** No-argument constructor */
    public DiscountBean() { }

    public String getDiscountId() { return discountId; }
    public void setDiscountId(String discountId) { this.discountId = discountId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the discounts table. */
    public static class DAO {

        /** Persists a DiscountBean to the discounts table. */
        public void save(DiscountBean discount) {
            String sql = "INSERT OR REPLACE INTO discounts (discountId,saleId,discountType,discountValue,description) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, discount.getDiscountId());
                ps.setString(2, discount.getSaleId());
                ps.setString(3, discount.getDiscountType());
                ps.setDouble(4, discount.getDiscountValue());
                ps.setString(5, discount.getDescription());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save discount: " + e.getMessage());
            }
        }

        /** Finds a DiscountBean by sale ID, or returns null if not found. */
        public DiscountBean findBySaleId(String saleId) {
            String sql = "SELECT * FROM discounts WHERE saleId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        DiscountBean d = new DiscountBean();
                        d.setDiscountId(rs.getString("discountId"));
                        d.setSaleId(rs.getString("saleId"));
                        d.setDiscountType(rs.getString("discountType"));
                        d.setDiscountValue(rs.getDouble("discountValue"));
                        d.setDescription(rs.getString("description"));
                        return d;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find discount: " + e.getMessage());
            }
            return null;
        }
    }
}

// reviewed by: Omar Khalifa
