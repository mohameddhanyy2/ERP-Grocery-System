package com.groceryerp.pos.beans;
import java.io.Serializable;

/** JavaBean representing a single line item within a sale. */
public class SaleItemBean implements Serializable {
    private String itemId;
    private String saleId;
    private String productId;
    private int quantity;
    private double unitPrice;
    private double lineTotal;

    public SaleItemBean() {}

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}
