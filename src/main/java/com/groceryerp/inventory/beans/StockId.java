package com.groceryerp.inventory.beans;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link StockBean}: (storeId, productId).
 * Required by JPA @IdClass.
 */
public class StockId implements Serializable {

    private String storeId;
    private String productId;

    public StockId() {}

    public StockId(String storeId, String productId) {
        this.storeId = storeId;
        this.productId = productId;
    }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockId)) return false;
        StockId that = (StockId) o;
        return Objects.equals(storeId, that.storeId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId, productId);
    }
}
