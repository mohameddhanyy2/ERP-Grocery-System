package com.groceryerp.reporting.beans;
import java.io.Serializable;

/** JavaBean carrying a snapshot of inventory status across stores. */
public class InventoryReportBean implements Serializable {
    private int totalProducts;
    private int lowStockCount;
    private int expiredItemCount;
    private double stockValue;
    private String generatedAt;

    public InventoryReportBean() {}

    public int getTotalProducts() { return totalProducts; }
    public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }
    public int getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; }
    public int getExpiredItemCount() { return expiredItemCount; }
    public void setExpiredItemCount(int expiredItemCount) { this.expiredItemCount = expiredItemCount; }
    public double getStockValue() { return stockValue; }
    public void setStockValue(double stockValue) { this.stockValue = stockValue; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
}
