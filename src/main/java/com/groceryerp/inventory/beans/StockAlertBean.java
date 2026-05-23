package com.groceryerp.inventory.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaBean representing a low-stock alert raised for one product in one store.
 * <p>
 * Follows the JavaBeans specification: public class, public no-argument
 * constructor, all fields private, and a public getter/setter for every
 * field. Implements {@link Serializable} so the alert can be transferred
 * between components (for example to the Supplier module).
 */
public class StockAlertBean implements Serializable {

    //@Id
    private String alertId; // Unique identifier for the alert, e.g. "ALERT-1234567890".
    /** Product code the alert is about. */
    private String productId;
    /** Store the alert was raised in. */
    private String storeId;
    /** Quantity currently on hand when the alert was raised. */
    private int currentQty;
    /** Threshold below which the alert fires. */
    private int threshold;
    /** Date the alert was raised, e.g. "2026-05-23". */
    private String alertDate;

    /** Public no-argument constructor required by the JavaBeans spec. */
    public StockAlertBean() { /* no-arg constructor required by JavaBeans spec */ }

    // ── JavaBean accessors ──────────────────────────────────────────
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    /** @return the product code the alert is about. */
    public String getProductId() { return productId; }

    /** @param productId the product code to set. */
    public void setProductId(String productId) { this.productId = productId; }

    /** @return the store the alert was raised in. */
    public String getStoreId() { return storeId; }

    /** @param storeId the store id to set. */
    public void setStoreId(String storeId) { this.storeId = storeId; }

    /** @return the quantity on hand when the alert was raised. */
    public int getCurrentQty() { return currentQty; }

    /** @param currentQty the current quantity to set. */
    public void setCurrentQty(int currentQty) { this.currentQty = currentQty; }

    /** @return the threshold below which the alert fires. */
    public int getThreshold() { return threshold; }

    /** @param threshold the threshold to set. */
    public void setThreshold(int threshold) { this.threshold = threshold; }

    /** @return the date the alert was raised. */
    public String getAlertDate() { return alertDate; }

    /** @param alertDate the alert date to set. */
    public void setAlertDate(String alertDate) { this.alertDate = alertDate; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles persistence for stock_alerts and low-stock queries against the stock table. */
    public static class DAO {

        /** Persists a StockAlertBean to the stock_alerts table. */
        public void save(StockAlertBean alert) {
            String sql = "INSERT OR REPLACE INTO stock_alerts (alertId,productId,storeId,currentQty,threshold,alertDate) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, alert.getAlertId());
                ps.setString(2, alert.getProductId());
                ps.setString(3, alert.getStoreId());
                ps.setInt(4, alert.getCurrentQty());
                ps.setInt(5, alert.getThreshold());
                ps.setString(6, alert.getAlertDate());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save stock alert: " + e.getMessage());
            }
        }

        /** Returns all stock alerts for a given store. */
        public List<StockAlertBean> findByStore(String storeId) {
            List<StockAlertBean> list = new ArrayList<>();
            String sql = "SELECT alertId,productId,storeId,currentQty,threshold,alertDate FROM stock_alerts WHERE storeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        StockAlertBean a = new StockAlertBean();
                        a.setAlertId(rs.getString("alertId"));
                        a.setProductId(rs.getString("productId"));
                        a.setStoreId(rs.getString("storeId"));
                        a.setCurrentQty(rs.getInt("currentQty"));
                        a.setThreshold(rs.getInt("threshold"));
                        a.setAlertDate(rs.getString("alertDate"));
                        list.add(a);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find stock alerts: " + e.getMessage());
            }
            return list;
        }

        /** Deletes all alerts for a given product and store (called when a restock alert is resolved). */
        public void deleteByProductAndStore(String productId, String storeId) {
            String sql = "DELETE FROM stock_alerts WHERE productId = ? AND storeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, productId);
                ps.setString(2, storeId);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to delete stock alert: " + e.getMessage());
            }
        }

        /**
         * Returns product IDs in the stock table where quantity is below the threshold for a given store.
         * Queries the stock table directly (not stock_alerts).
         */
        public List<String> findLowStockProductIds(String storeId, int threshold) {
            List<String> list = new ArrayList<>();
            String sql = "SELECT productId FROM stock WHERE storeId = ? AND quantity < ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                ps.setInt(2, threshold);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { list.add(rs.getString("productId")); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find low stock products: " + e.getMessage());
            }
            return list;
        }
    }
}

// conflicts resolved by: Omar Khalifa