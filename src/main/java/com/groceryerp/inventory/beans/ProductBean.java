package com.groceryerp.inventory.beans;
import java.io.Serializable;

/** JavaBean representing a product in the inventory. */
public class ProductBean implements Serializable {
    private String productId;
    private String name;
    private String category;
    private double price;
    private String expiryDate;

    public ProductBean() {}
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}
