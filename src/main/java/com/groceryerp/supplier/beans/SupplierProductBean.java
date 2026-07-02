package com.groceryerp.supplier.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * SupplierProductBean — JPA entity mapped to the {@code supplier_products} join
 * table (which suppliers carry which products). Composite primary key
 * (supplierId, productId) via {@link SupplierProductId}.
 *
 * Previously {@code supplier_products} had no @Entity and was managed by
 * SQLite-specific native SQL ({@code INSERT OR IGNORE}). Promoted to an entity so
 * the assign/remove logic is portable JPA and Hibernate creates the table on
 * Postgres with the composite PK.
 */
@Entity
@Table(name = "supplier_products")
@IdClass(SupplierProductId.class)
public class SupplierProductBean implements Serializable {

    @Id
    @Column(name = "supplierId")
    private String supplierId;

    @Id
    @Column(name = "productId")
    private String productId;

    /** Purchase price this supplier charges for the product (nullable for rows that predate this column). */
    @Column(name = "unitCost")
    private Double unitCost;

    public SupplierProductBean() {}

    public SupplierProductBean(String supplierId, String productId) {
        this.supplierId = supplierId;
        this.productId = productId;
    }

    public SupplierProductBean(String supplierId, String productId, Double unitCost) {
        this.supplierId = supplierId;
        this.productId = productId;
        this.unitCost = unitCost;
    }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public double getUnitCost() { return unitCost != null ? unitCost : 0.0; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }
}
