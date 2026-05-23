package com.groceryerp.supplier.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="order_lines")
/**
 * OrderLineBean — Entity Bean mapped to the {@code order_lines} table.
 * One row per line item within a purchase order. Bean type: @Entity.
 */
public class OrderLineBean implements Serializable {

    // @Id
    /** Unique line identifier. */
    private String lineId;
    /** ID of the parent purchase order. */
    private String orderId;
    /** ID of the product being ordered. */
    private String productId;
    /** Quantity ordered. */
    private int quantity;
    /** Unit price agreed with supplier. */
    private double unitPrice;

    /** No-argument constructor required by JavaBeans spec. */
    public OrderLineBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the order_lines table. */
    public static class DAO {

        /** Persists an OrderLineBean to the order_lines table. */
        public void save(OrderLineBean line) {
            String sql = "INSERT OR REPLACE INTO order_lines (lineId,orderId,productId,quantity,unitPrice) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, line.getLineId());
                ps.setString(2, line.getOrderId());
                ps.setString(3, line.getProductId());
                ps.setInt(4, line.getQuantity());
                ps.setDouble(5, line.getUnitPrice());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save order line: " + e.getMessage());
            }
        }

        /** Returns all order lines for a given order ID. */
        public List<OrderLineBean> findByOrderId(String orderId) {
            List<OrderLineBean> list = new ArrayList<>();
            String sql = "SELECT lineId,orderId,productId,quantity,unitPrice FROM order_lines WHERE orderId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        OrderLineBean l = new OrderLineBean();
                        l.setLineId(rs.getString("lineId"));
                        l.setOrderId(rs.getString("orderId"));
                        l.setProductId(rs.getString("productId"));
                        l.setQuantity(rs.getInt("quantity"));
                        l.setUnitPrice(rs.getDouble("unitPrice"));
                        list.add(l);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find order lines: " + e.getMessage());
            }
            return list;
        }
    }
}
