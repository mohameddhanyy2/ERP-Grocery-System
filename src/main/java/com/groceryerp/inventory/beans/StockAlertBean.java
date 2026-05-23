package com.groceryerp.inventory.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="stock_alerts")
/**
 * StockAlertBean — Entity Bean mapped to the {@code stock_alerts} table.
 * One row per low-stock alert event for a product at a store. Bean type: @Entity.
 */
public class StockAlertBean implements Serializable {

    // @Id
    /** Unique alert identifier. */
    private String alertId;
    /** Product that triggered the alert. */
    private String productId;
    /** Store where the low stock was detected. */
    private String storeId;
    /** Current quantity at the time of the alert. */
    private int currentQty;
    /** Threshold below which the alert was raised. */
    private int threshold;
    /** ISO-8601 date when the alert was generated. */
    private String alertDate;

    /** No-argument constructor required by JavaBeans spec. */
    public StockAlertBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public int getCurrentQty() { return currentQty; }
    public void setCurrentQty(int currentQty) { this.currentQty = currentQty; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    public String getAlertDate() { return alertDate; }
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
