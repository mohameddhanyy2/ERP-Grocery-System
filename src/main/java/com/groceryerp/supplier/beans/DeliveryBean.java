package com.groceryerp.supplier.beans;
import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="deliveries")
/**
 * DeliveryBean — Entity Bean mapped to the {@code deliveries} table.
 * One row per delivery record for a purchase order. Bean type: @Entity.
 */
public class DeliveryBean implements Serializable {

    // @Id
    /** Unique delivery identifier. */
    private String deliveryId;
    /** ID of the purchase order this delivery fulfils. */
    private String orderId;
    /** ISO-8601 date the delivery arrived, or null if pending. */
    private String deliveryDate;
    /** Delivery status: PENDING, DELIVERED, FAILED. */
    private String deliveryStatus;

    /** No-argument constructor required by JavaBeans spec. */
    public DeliveryBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the deliveries table. */
    public static class DAO {

        /** Persists a DeliveryBean to the deliveries table. */
        public void save(DeliveryBean delivery) {
            String sql = "INSERT OR REPLACE INTO deliveries (deliveryId,orderId,deliveryDate,deliveryStatus) VALUES (?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, delivery.getDeliveryId());
                ps.setString(2, delivery.getOrderId());
                ps.setString(3, delivery.getDeliveryDate());
                ps.setString(4, delivery.getDeliveryStatus());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save delivery: " + e.getMessage());
            }
        }

        /** Finds a DeliveryBean by order ID, or returns null if not found. */
        public DeliveryBean findByOrderId(String orderId) {
            String sql = "SELECT deliveryId,orderId,deliveryDate,deliveryStatus FROM deliveries WHERE orderId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        DeliveryBean d = new DeliveryBean();
                        d.setDeliveryId(rs.getString("deliveryId"));
                        d.setOrderId(rs.getString("orderId"));
                        d.setDeliveryDate(rs.getString("deliveryDate"));
                        d.setDeliveryStatus(rs.getString("deliveryStatus"));
                        return d;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find delivery: " + e.getMessage());
            }
            return null;
        }

        /** Updates the status of a delivery. */
        public void updateStatus(String deliveryId, String status) {
            String sql = "UPDATE deliveries SET deliveryStatus = ? WHERE deliveryId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, deliveryId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to update delivery status: " + e.getMessage());
            }
        }
    }
}

// conflicts resolved by: Omar Khalifa
