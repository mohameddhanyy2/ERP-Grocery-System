package com.groceryerp.inventory.beans;
import java.io.Serializable;

/**
 * JavaBean representing a single product carried by the grocery chain.
 * <p>
 * Follows the JavaBeans specification: public class, public no-argument
 * constructor, all fields private, and a public getter/setter for every
 * field. Implements {@link Serializable} so it can be passed between
 * components and packaged units.
 */
public class ProductBean implements Serializable {

    /** Unique product code, e.g. "PROD_001". */
    private String productId;
    /** Human-readable product name, e.g. "Whole Milk 1L". */
    private String name;
    /** Product category, e.g. "Dairy". */
    private String category;
    /** Shelf price in the store currency. */
    private double price;
    /** Expiry date as a plain string, e.g. "2026-08-01". */
    private String expiryDate;

    /** Public no-argument constructor required by the JavaBeans spec. */
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
}
