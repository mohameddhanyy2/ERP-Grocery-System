package com.groceryerp.inventory.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * StockAlertBean — JPA entity mapped to the {@code stock_alerts} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL against {@code stock_alerts} (save / findByStore /
 * deleteByProductAndStore) and against the {@code stock} table
 * (findLowStockProductIds). The DAO is gone — persistence is now handled by
 * {@link com.groceryerp.inventory.InventoryRepository} via the container-managed
 * EntityManager. The annotations below ARE the table mapping (table name
 * {@code stock_alerts} taken from the original DAO SQL).
 */
@Entity
@Table(name = "stock_alerts")
public class StockAlertBean implements Serializable {

    /** Unique identifier for the alert, e.g. "ALERT-1234567890". */
    @Id
    @Column(name = "alertId")
    private String alertId;

    /** Product code the alert is about. */
    @Column(name = "productId")
    private String productId;

    /** Store the alert was raised in. */
    @Column(name = "storeId")
    private String storeId;

    /** Quantity currently on hand when the alert was raised. */
    @Column(name = "currentQty")
    private int currentQty;

    /** Threshold below which the alert fires. */
    @Column(name = "threshold")
    private int threshold;

    /** Date the alert was raised, e.g. "2026-05-23". */
    @Column(name = "alertDate")
    private String alertDate;

    /** Public no-argument constructor required by the JavaBeans / JPA spec. */
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
}

// conflicts resolved by: Omar Khalifa
