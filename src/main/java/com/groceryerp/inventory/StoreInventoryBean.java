package com.groceryerp.inventory;

import com.groceryerp.interfaces.IStoreInventory;
import java.io.Serializable;
import java.util.*;

/**
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
    /** Maps each product code to the quantity currently on hand. */
    private Map<String, Integer> stockMap;
    /** Quantity at or below which a product counts as low stock. */
    private int lowStockThreshold;

    /**
     * Public no-argument constructor required by the JavaBeans spec.
     * Starts with an empty stock map and a default threshold of 10.
     */
    public StoreInventoryBean() {
        this.stockMap = new HashMap<>();
        this.lowStockThreshold = 10;
    }

    // ── IStoreInventory implementation ─────────────────────────────

    /**
     * Returns how many units of a product this branch holds.
     *
     * @param productId the product code to look up.
     * @return the quantity on hand, or 0 if the product is unknown.
     */
    @Override
    public int checkStock(String productId) {
        return stockMap.getOrDefault(productId, 0);
    }

    /**
     * Adjusts the quantity of a product. A positive value adds stock
     * (a delivery), a negative value removes it (a sale).
     *
     * @param productId the product code to adjust.
     * @param quantity  the signed change to apply to the current quantity.
     */
    @Override
    public void updateStock(String productId, int quantity) {
        int current = stockMap.getOrDefault(productId, 0);
        stockMap.put(productId, current + quantity);
    }

    /**
     * Lists every product whose quantity is below {@link #lowStockThreshold}.
     *
     * @return the product codes that currently need restocking.
     */
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

    /** @return the unique branch code. */
    @Override
    public String getStoreId() { return storeId; }

    // ── JavaBean accessors ──────────────────────────────────────────

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

    /**
     * Standalone test that verifies the {@link IStoreInventory} contract
     * works in isolation, without any other module. Run with:
     * <pre>java com.groceryerp.inventory.StoreInventoryBean</pre>
     */
    public static void main(String[] args) {
        System.out.println("=== StoreInventoryBean — IStoreInventory contract test ===\n");

        // Build one store branch via JavaBean setters.
        StoreInventoryBean store = new StoreInventoryBean();
        store.setStoreId("Seoudi_Z");
        store.setStoreName("Zayed Branch");
        store.setLowStockThreshold(10);

        // Reference it through the INTERFACE — proves the contract holds.
        IStoreInventory inv = store;

        // updateStock + checkStock
        inv.updateStock("PROD_001", 50);
        inv.updateStock("PROD_002", 4);

        System.out.println("getStoreId()           -> " + inv.getStoreId());
        System.out.println("checkStock(PROD_001)   -> " + inv.checkStock("PROD_001"));
        System.out.println("checkStock(PROD_999)   -> " + inv.checkStock("PROD_999") + "  (unknown product)");

        // updateStock with a negative value = a sale
        inv.updateStock("PROD_001", -8);
        System.out.println("checkStock(PROD_001)   -> " + inv.checkStock("PROD_001") + "  (after selling 8)");

        // getLowStockAlerts — PROD_002 (qty 4) is below the threshold of 10
        System.out.println("getLowStockAlerts()    -> " + inv.getLowStockAlerts());

        // Simple pass/fail checks
        boolean ok = inv.checkStock("PROD_001") == 42
                && inv.checkStock("PROD_999") == 0
                && inv.getLowStockAlerts().contains("PROD_002")
                && !inv.getLowStockAlerts().contains("PROD_001");
        System.out.println("\nContract verified: " + (ok ? "PASS" : "FAIL"));
    }
}
