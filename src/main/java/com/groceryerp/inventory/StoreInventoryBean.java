package com.groceryerp.inventory;

import com.groceryerp.interfaces.IStoreInventory;
import java.io.Serializable;
import java.util.*;

/**
 * Leaf component of the Composite Structure.
 * Represents inventory for ONE physical store branch.
 * Implements IStoreInventory — the shared contract.
 */
public class StoreInventoryBean implements IStoreInventory, Serializable {

    private String storeId;
    private String storeName;
    private Map<String, Integer> stockMap;
    private int lowStockThreshold;

    public StoreInventoryBean() {
        this.stockMap = new HashMap<>();
        this.lowStockThreshold = 10;
    }

    // ── IStoreInventory implementation ─────────────────────────────
    @Override
    public int checkStock(String productId) {
        return stockMap.getOrDefault(productId, 0);
    }

    @Override
    public void updateStock(String productId, int quantity) {
        int current = stockMap.getOrDefault(productId, 0);
        stockMap.put(productId, current + quantity);
    }

    @Override
    public List<String> getLowStockAlerts() {
        List<String> alerts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            if (entry.getValue() < lowStockThreshold) {
                alerts.add(entry.getKey());
            }
        }
        return alerts;
    }

    @Override
    public String getStoreId() { return storeId; }

    // ── JavaBean accessors ──────────────────────────────────────────
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Map<String, Integer> getStockMap() { return stockMap; }
    public void setStockMap(Map<String, Integer> stockMap) { this.stockMap = stockMap; }
}
