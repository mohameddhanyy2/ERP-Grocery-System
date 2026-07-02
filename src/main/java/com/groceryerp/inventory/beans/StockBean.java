package com.groceryerp.inventory.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * StockBean — JPA entity mapped to the {@code stock} table (on-hand quantity of a
 * product at a store). Composite primary key (storeId, productId) via {@link StockId}.
 *
 * Previously {@code stock} had no @Entity and was managed by SQLite-specific native
 * SQL (the {@code ON CONFLICT ... DO UPDATE} upsert). Promoted to an entity so the
 * adjust-stock logic becomes a portable read-modify-write and Hibernate creates the
 * table with the composite PK on Postgres.
 */
@Entity
@Table(name = "stock")
@IdClass(StockId.class)
public class StockBean implements Serializable {

    @Id
    @Column(name = "storeId")
    private String storeId;

    @Id
    @Column(name = "productId")
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public StockBean() {}

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
