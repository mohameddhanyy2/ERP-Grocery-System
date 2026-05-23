package com.groceryerp.supplier.beans;

import java.io.Serializable;

/**
 * JavaBean — represents a purchase order sent to a supplier.
 * One order can cover multiple products via OrderLineBean entries.
 */
public class PurchaseOrderBean implements Serializable {

    private String orderId;
    private String supplierId;
    private String storeId;
    private String orderDate;
    private double totalCost;
    private String status;  // PENDING | DELIVERED

    public PurchaseOrderBean() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "PurchaseOrderBean{orderId='" + orderId + "', supplierId='" + supplierId +
               "', storeId='" + storeId + "', orderDate='" + orderDate +
               "', totalCost=" + totalCost + ", status='" + status + "'}";
    }
}
