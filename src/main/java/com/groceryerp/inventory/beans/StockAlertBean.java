package com.groceryerp.inventory.beans;
import java.io.Serializable;

/** JavaBean representing a low-stock alert for a product in a store. */
public class StockAlertBean implements Serializable {
    private String productId;
    private String storeId;
    private int currentQty;
    private int threshold;
    private String alertDate;

    public StockAlertBean() {}
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
}
