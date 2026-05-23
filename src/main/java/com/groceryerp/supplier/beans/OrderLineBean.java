package com.groceryerp.supplier.beans;

import java.io.Serializable;

/**
 * JavaBean — represents one product line inside a PurchaseOrderBean.
 * A single purchase order can have multiple order lines (one per product).
 */
public class OrderLineBean implements Serializable {

    private String orderId;
    private String productId;
    private int quantity;
    private double unitPrice;

    public OrderLineBean() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getLineTotal() { return quantity * unitPrice; }

    @Override
    public String toString() {
        return "OrderLineBean{orderId='" + orderId + "', productId='" + productId +
               "', quantity=" + quantity + ", unitPrice=" + unitPrice + "}";
    }
}
