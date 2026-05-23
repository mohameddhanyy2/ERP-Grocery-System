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
import java.util.Map;

/*
 * Leaf component of the inventory Composite Structure.
 * <p>
 * Represents the stock held by ONE physical store branch. It implements
 * {@link IStoreInventory} — the shared contract delivered by Member 1 — so
 * that callers (POS, Supplier, and the {@link CentralInventoryBean}
 * composite) can treat one store and a whole chain through the same type.
 * <p>
 * The class is also a JavaBean: public class, public no-argument
 * constructor, private fields, public getters/setters, and
 * {@link Serializable}.
 */
public class StoreInventoryBean implements IStoreInventory, Serializable {

    /** Unique branch code, e.g. "STORE_A". */
    private String storeId;
    /** Human-readable branch name, e.g. "Downtown Branch". */
    private String storeName;

    // Required dependency — injected via setter (IoC)
    private StockAlertMDB stockAlertMDB;

    /** Maps each product code to the quantity currently on hand. */
    private Map<String, Integer> stockMap;

    /** Quantity at or below which a product counts as low stock. */
    private int lowStockThreshold;

    /**
     * Public no-argument constructor required by the JavaBeans spec.
     * Starts with an empty stock map and a default threshold of 10.
     */
    public StoreInventoryBean() {
        this.lowStockThreshold = 10;
    }

    /** Injects the MDB that receives low-stock alert messages. */
    public void setStockAlertMDB(StockAlertMDB stockAlertMDB) {
        this.stockAlertMDB = stockAlertMDB;
    }

    // ── JavaBean accessors ──────────────────────────────────────────

    /** @return the unique branch code. */
    @Override
    public String getStoreId() { return storeId; }
    
    /** @param storeId the unique branch code to set. */
    public void setStoreId(String storeId) { this.storeId = storeId; }

    /** @return the human-readable branch name. */
    public String getStoreName() { return storeName; }

    /** @param storeName the branch name to set. */
    public void setStoreName(String storeName) { this.storeName = storeName; }

    /** @return the low-stock threshold. */
    public int getLowStockThreshold() { return lowStockThreshold; }

    /** @param lowStockThreshold the low-stock threshold to set. */
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    /** @return the product-to-quantity stock map. */
    public Map<String, Integer> getStockMap() { return stockMap; }

    /** @param stockMap the product-to-quantity stock map to set. */
    public void setStockMap(Map<String, Integer> stockMap) { this.stockMap = stockMap; }

    // ── IStoreInventory implementation ─────────────────────────────

    /**
     * Returns how many units of a product this branch holds.
     *
     * @param productId the product code to look up.
     * @return the quantity on hand, or 0 if the product is unknown.
     */

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

    /**
     * Adjusts the quantity of a product. A positive value adds stock
     * (a delivery), a negative value removes it (a sale).
     *
     * @param productId the product code to adjust.
     * @param quantity  the signed change to apply to the current quantity.
     */
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

    /**
     * Lists every product whose quantity is below {@link #lowStockThreshold}.
     *
     * @return the product codes that currently need restocking.
     */
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


    // ── @Remove — session end method ─────────────────────────────

    // @Remove
    /** Clears all session state. Call when this store's session is no longer needed. */
    public void closeStore() {
        storeId = null;
        storeName = null;
        stockAlertMDB = null;
    }
}
