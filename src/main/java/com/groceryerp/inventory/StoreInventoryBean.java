package com.groceryerp.inventory;

import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.inventory.beans.StockAlertBean;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * Leaf component of the inventory Composite Structure.
 * <p>
 * Represents the stock held by ONE physical store branch. It implements
 * {@link IStoreInventory} so that callers (POS, Supplier, and the
 * {@link CentralInventoryBean} composite) can treat one store and a whole chain
 * through the same type.
 * <p>
 * Kept as a {@code @Dependent} CDI bean rather than promoted to an @Entity:
 * storeId / storeName / lowStockThreshold are per-store session configuration,
 * not a persisted row. All persistence (the old raw {@code stock} / stock_alerts
 * SQL) is delegated to the injected @Stateless {@link InventoryRepository}.
 */
@Dependent
public class StoreInventoryBean implements IStoreInventory, Serializable {

    /** Unique branch code, e.g. "STORE_A". */
    private String storeId;
    /** Human-readable branch name, e.g. "Downtown Branch". */
    private String storeName;

    // Container-injected persistence service (replaces the nested DAO usage
    // and the raw JDBC that used to live in checkStock()/updateStock()).
    @Inject
    private InventoryRepository repository;

    // Low-stock alert sink. Was a direct reference to the fake "MDB" object whose
    // onMessage(String,Object) was called synchronously like any other method.
    // Now the container-injected JMS producer: updateStock() publishes an
    // ObjectMessage to the StockAlert queue and returns immediately; the real
    // @MessageDriven StockAlertMDB consumes it asynchronously.
    @Inject
    private StockAlertProducer stockAlertProducer;

    /** Quantity at or below which a product counts as low stock. */
    private int lowStockThreshold;

    /**
     * Public no-argument constructor required by the JavaBeans / CDI spec.
     * Starts with a default threshold of 10.
     */
    public StoreInventoryBean() {
        this.lowStockThreshold = 10;
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

    // ── IStoreInventory implementation ─────────────────────────────

    /**
     * Returns how many units of a product this branch holds.
     *
     * @param productId the product code to look up.
     * @return the quantity on hand, or 0 if the product is unknown.
     */
    @Override
    public int checkStock(String productId) {
        // Was raw SQL: SELECT quantity FROM stock WHERE storeId = ? AND productId = ?
        return repository.getStockQuantity(storeId, productId);
    }

    /**
     * Adjusts the quantity of a product. A positive value adds stock
     * (a delivery), a negative value removes it (a sale).
     *
     * @param productId the product code to adjust.
     * @param delta     the signed change to apply to the current quantity.
     */
    @Override
    public void updateStock(String productId, int delta) {
        // Was raw SQLite UPSERT SQL — now in InventoryRepository.adjustStock().
        repository.adjustStock(storeId, productId, delta);

        // After update: check if quantity dropped below threshold and fire alert
        if (delta < 0 && stockAlertProducer != null) {
            int newQty = checkStock(productId);
            if (newQty < lowStockThreshold) {
                StockAlertBean alert = new StockAlertBean();
                alert.setAlertId("ALERT-" + storeId + "-" + productId + "-" + System.currentTimeMillis());
                alert.setProductId(productId);
                alert.setStoreId(storeId);
                alert.setCurrentQty(newQty);
                alert.setThreshold(lowStockThreshold);
                alert.setAlertDate(LocalDate.now().toString());
                // Publish to the JMS queue; the real MDB consumes it asynchronously.
                stockAlertProducer.fireLowStock(alert);
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
        // Was: new StockAlertBean.DAO().findLowStockProductIds(...)
        List<String> lowIds = repository.findLowStockProductIds(storeId, lowStockThreshold);
        List<String> messages = new ArrayList<>();
        for (String productId : lowIds) {
            messages.add("LOW STOCK: " + productId + " at " + storeId);
        }
        return messages;
    }

    // ── session end method ───────────────────────────────────────

    /** Clears all session state. Call when this store's session is no longer needed. */
    public void closeStore() {
        storeId = null;
        storeName = null;
    }
}
