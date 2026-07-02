package com.groceryerp.inventory.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * StoreBean — JPA entity mapped to the {@code stores} table.
 *
 * Previously {@code stores} had no @Entity and was reached only through native
 * SQL (DatabaseManager DDL + InventoryRepository native queries). Promoted to a
 * real entity so Hibernate creates the table on any database (Postgres/Neon) and
 * the load/save go through portable JPA instead of dialect-specific SQL.
 */
@Entity
@Table(name = "stores")
public class StoreBean implements Serializable {

    @Id
    @Column(name = "storeId")
    private String storeId;

    @Column(name = "storeName", nullable = false)
    private String storeName;

    public StoreBean() {}

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
}
