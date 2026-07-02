package com.groceryerp.inventory.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * ProductBean — JPA entity mapped to the {@code products} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL ("INSERT OR REPLACE INTO products ..." / SELECT). The DAO is gone —
 * persistence is now handled by {@link com.groceryerp.inventory.InventoryRepository}
 * via the container-managed EntityManager. The annotations below ARE the table
 * mapping (table name {@code products} taken from the original DAO SQL).
 */
@Entity
@Table(name = "products")
public class ProductBean implements Serializable {

    /** Unique product code, e.g. "PROD_001". */
    @Id
    @Column(name = "productId")
    private String productId;

    /** Human-readable product name, e.g. "Whole Milk 1L". */
    @Column(name = "name")
    private String name;

    /** Product category, e.g. "Dairy". */
    @Column(name = "category")
    private String category;

    /** Shelf price in the store currency. */
    @Column(name = "price")
    private double price;

    /** Expiry date as a plain string, e.g. "2026-08-01". */
    @Column(name = "expiryDate")
    private String expiryDate;

    /** Barcode number (manually entered, e.g. EAN-13). */
    @Column(name = "barcode")
    private String barcode;

    /** Primary supplier for this product (FK to suppliers.supplierId). */
    @Column(name = "supplierId")
    private String supplierId;

    /** Purchase/unit cost from the supplier. Stored as Double (nullable) so existing
     *  rows with NULL in the new column don't throw a primitive-assignment error. */
    @Column(name = "unitCost")
    private Double unitCost;

    /** Public no-argument constructor required by the JavaBeans / JPA spec. */
    public ProductBean() {}

    /** @return the unique product code. */
    public String getProductId() { return productId; }

    /** @param productId the unique product code to set. */
    public void setProductId(String productId) { this.productId = productId; }

    /** @return the human-readable product name. */
    public String getName() { return name; }

    /** @param name the product name to set. */
    public void setName(String name) { this.name = name; }

    /** @return the product category. */
    public String getCategory() { return category; }

    /** @param category the product category to set. */
    public void setCategory(String category) { this.category = category; }

    /** @return the shelf price. */
    public double getPrice() { return price; }

    /** @param price the shelf price to set. */
    public void setPrice(double price) { this.price = price; }

    /** @return the expiry date string. */
    public String getExpiryDate() { return expiryDate; }

    /** @param expiryDate the expiry date string to set. */
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public double getUnitCost() { return unitCost != null ? unitCost : 0.0; }
    public void setUnitCost(double unitCost) { this.unitCost = unitCost; }
}

// conflicts resolved by: Omar Khalifa
