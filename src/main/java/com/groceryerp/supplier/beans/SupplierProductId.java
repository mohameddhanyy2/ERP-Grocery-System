package com.groceryerp.supplier.beans;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link SupplierProductBean}: (supplierId, productId).
 * Required by JPA @IdClass.
 */
public class SupplierProductId implements Serializable {

    private String supplierId;
    private String productId;

    public SupplierProductId() {}

    public SupplierProductId(String supplierId, String productId) {
        this.supplierId = supplierId;
        this.productId = productId;
    }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierProductId)) return false;
        SupplierProductId that = (SupplierProductId) o;
        return Objects.equals(supplierId, that.supplierId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId, productId);
    }
}
