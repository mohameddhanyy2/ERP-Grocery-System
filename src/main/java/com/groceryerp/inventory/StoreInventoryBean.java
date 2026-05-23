package com.groceryerp.inventory;

// @Stateful
// Chosen because storeId, storeName, and lowStockThreshold are session-level
// configuration set once at setup time and used across all subsequent calls.
// checkStock() and updateStock() rely on storeId from the session — they are
// not passed storeId as a parameter, so this bean must remember it.

import com.groceryerp.db.DatabaseManager;
import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.inventory.beans.StockAlertBean;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * StoreInventoryBean — Stateful Session Bean representing inventory for one store branch.
 *
 * Session state: storeId, storeName, lowStockThreshold — set at setup time via setters
 * and used across all subsequent checkStock() / updateStock() calls within this session.
 * Implements IStoreInventory — the shared contract used by POSModule and SupplierModule.
 *
 * Bean type: @Stateful — holds per-store configuration as conversational state.
 * The caller (Main.java) configures the bean once; all subsequent calls use that state.
 */
public class StoreInventoryBean implements IStoreInventory, Serializable {

    // ── Session state fields ──────────────────────────────────────
    private String storeId;
    private String storeName;
    private int lowStockThreshold;

    // Required dependency — injected via setter (IoC)
    private StockAlertMDB stockAlertMDB;

    public StoreInventoryBean() {
        this.lowStockThreshold = 10;
    }

    /** Injects the MDB that receives low-stock alert messages. */
    public void setStockAlertMDB(StockAlertMDB stockAlertMDB) {
        this.stockAlertMDB = stockAlertMDB;
    }

    // ── IStoreInventory implementation ────────────────────────────

    @Override
    public int checkStock(String productId) {
        String sql = "SELECT quantity FROM stock WHERE storeId = ? AND productId = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, storeId);
            ps.setString(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { return rs.getInt("quantity"); }
            }
        } catch (SQLException e) {
            System.out.println("Failed to check stock: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void updateStock(String productId, int delta) {
        String upsert = "INSERT INTO stock (storeId,productId,quantity) VALUES (?,?,?)"
                + " ON CONFLICT(storeId,productId) DO UPDATE SET quantity = quantity + excluded.quantity";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(upsert)) {
            ps.setString(1, storeId);
            ps.setString(2, productId);
            ps.setInt(3, delta);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to update stock: " + e.getMessage());
        }

        // After update: check if quantity dropped below threshold and fire alert
        if (delta < 0 && stockAlertMDB != null) {
            int newQty = checkStock(productId);
            if (newQty < lowStockThreshold) {
                StockAlertBean alert = new StockAlertBean();
                alert.setAlertId("ALERT-" + storeId + "-" + productId + "-" + System.currentTimeMillis());
                alert.setProductId(productId);
                alert.setStoreId(storeId);
                alert.setCurrentQty(newQty);
                alert.setThreshold(lowStockThreshold);
                alert.setAlertDate(LocalDate.now().toString());
                stockAlertMDB.onMessage("LOW_STOCK", alert);
            }
        }
    }

    @Override
    public List<String> getLowStockAlerts() {
        StockAlertBean.DAO alertDao = new StockAlertBean.DAO();
        List<String> lowIds = alertDao.findLowStockProductIds(storeId, lowStockThreshold);
        List<String> messages = new ArrayList<>();
        for (String productId : lowIds) {
            messages.add("LOW STOCK: " + productId + " at " + storeId);
        }
        return messages;
    }

    @Override
    public String getStoreId() { return storeId; }

    // ── @Remove — session end method ─────────────────────────────

    // @Remove
    /** Clears all session state. Call when this store's session is no longer needed. */
    public void closeStore() {
        storeId = null;
        storeName = null;
        stockAlertMDB = null;
    }

    // ── JavaBean accessors ────────────────────────────────────────
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
}
