package com.groceryerp.supplier.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * PurchaseOrderBean — JPA entity mapped to the {@code purchase_orders} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT / UPDATE / SUM). The DAO is gone —
 * persistence is now handled by {@link com.groceryerp.supplier.SupplierRepository}
 * via the container-managed EntityManager. The annotations below ARE the mapping.
 *
 * Note: the original servlet's quote flow also wrote a {@code productAlertId}
 * column linking an order back to the stock alert that triggered it. That column
 * is mapped here so the supplier-portal quote/alert join keeps working.
 */
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrderBean implements Serializable {

    @Id
    @Column(name = "orderId")
    private String orderId;

    @Column(name = "supplierId")
    private String supplierId;

    @Column(name = "storeId")
    private String storeId;

    @Column(name = "orderDate")
    private String orderDate;

    @Column(name = "totalCost")
    private double totalCost;

    @Column(name = "status")
    private String status;

    /** Links this order back to the stock_alerts row that triggered it (quote flow). */
    @Column(name = "productAlertId")
    private String productAlertId;

    public PurchaseOrderBean() { /* no-arg constructor required by JPA */ }

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

    public String getProductAlertId() { return productAlertId; }
    public void setProductAlertId(String productAlertId) { this.productAlertId = productAlertId; }
}
