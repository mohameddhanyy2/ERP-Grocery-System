package com.groceryerp.inventory.beans;
import java.io.Serializable;

/**
 * JavaBean representing a low-stock alert raised for one product in one store.
 * <p>
 * Follows the JavaBeans specification: public class, public no-argument
 * constructor, all fields private, and a public getter/setter for every
 * field. Implements {@link Serializable} so the alert can be transferred
 * between components (for example to the Supplier module).
 */
public class StockAlertBean implements Serializable {

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
    public StockAlertBean() {}

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
