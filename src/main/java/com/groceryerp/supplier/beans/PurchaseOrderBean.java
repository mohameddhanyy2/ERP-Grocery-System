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
// @Table(name="purchase_orders")
/**
 * PurchaseOrderBean — Entity Bean mapped to the {@code purchase_orders} table.
 * One row per purchase order placed with a supplier. Bean type: @Entity.
 */
public class PurchaseOrderBean implements Serializable {

    // @Id
    /** Unique order identifier, e.g. "ORD-1234567890". */
    private String orderId;
    /** ID of the supplier fulfilling the order. */
    private String supplierId;
    /** ID of the store receiving the order. */
    private String storeId;
    /** ISO-8601 date the order was placed. */
    private String orderDate;
    /** Total cost of all lines in the order. */
    private double totalCost;
    /** Order status: PENDING, CONFIRMED, DELIVERED, CANCELLED. */
    private String status;

    /** No-argument constructor required by JavaBeans spec. */
    public PurchaseOrderBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the purchase_orders table. */
    public static class DAO {

        /** Persists a PurchaseOrderBean to the purchase_orders table. */
        public void save(PurchaseOrderBean order) {
            String sql = "INSERT OR REPLACE INTO purchase_orders (orderId,supplierId,storeId,orderDate,totalCost,status) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, order.getOrderId());
                ps.setString(2, order.getSupplierId());
                ps.setString(3, order.getStoreId());
                ps.setString(4, order.getOrderDate());
                ps.setDouble(5, order.getTotalCost());
                ps.setString(6, order.getStatus());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save purchase order: " + e.getMessage());
            }
        }

        public List<PurchaseOrderBean> findAll(){
            List<PurchaseOrderBean> list = new ArrayList<>();
            String sql = "SELECT orderId,supplierId,storeId,orderDate,totalCost,status FROM purchase_orders";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { list.add(mapRow(rs)); }
            } catch (SQLException e) {
                System.out.println("Failed to list purchase orders: " + e.getMessage());
            }
            return list;
        }

        /** Finds a PurchaseOrderBean by ID, or returns null if not found. */
        public PurchaseOrderBean findById(String orderId) {
            String sql = "SELECT orderId,supplierId,storeId,orderDate,totalCost,status FROM purchase_orders WHERE orderId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return mapRow(rs); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find purchase order: " + e.getMessage());
            }
            return null;
        }

        /** Returns all order IDs for a given store. */
        public List<String> findIdsByStore(String storeId) {
            List<String> list = new ArrayList<>();
            String sql = "SELECT orderId FROM purchase_orders WHERE storeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { list.add(rs.getString("orderId")); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to list order IDs: " + e.getMessage());
            }
            return list;
        }

        /** Updates the status of a purchase order. */
        public void updateStatus(String orderId, String status) {
            String sql = "UPDATE purchase_orders SET status = ? WHERE orderId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, orderId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to update order status: " + e.getMessage());
            }
        }

        /** Returns the sum of totalCost for orders whose orderDate matches the given period prefix. */
        public double sumCostByPeriod(String period) {
            String sql = "SELECT SUM(totalCost) FROM purchase_orders WHERE orderDate LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, period + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return rs.getDouble(1); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to sum order cost: " + e.getMessage());
            }
            return 0.0;
        }

        private PurchaseOrderBean mapRow(ResultSet rs) throws SQLException {
            PurchaseOrderBean o = new PurchaseOrderBean();
            o.setOrderId(rs.getString("orderId"));
            o.setSupplierId(rs.getString("supplierId"));
            o.setStoreId(rs.getString("storeId"));
            o.setOrderDate(rs.getString("orderDate"));
            o.setTotalCost(rs.getDouble("totalCost"));
            o.setStatus(rs.getString("status"));
            return o;
        } 

    }
}

// conflicts resolved by: Omar Khalifa