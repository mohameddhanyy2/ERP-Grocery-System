package com.groceryerp.supplier.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * OrderLineBean — JPA entity mapped to the {@code order_lines} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT). The DAO is gone — persistence is now
 * handled by {@link com.groceryerp.supplier.SupplierRepository} via the
 * container-managed EntityManager. The annotations below ARE the table mapping.
 */
@Entity
@Table(name = "order_lines")
public class OrderLineBean implements Serializable {

    @Id
    @Column(name = "lineId")
    private String lineId;

    @Column(name = "orderId")
    private String orderId;

    @Column(name = "productId")
    private String productId;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "unitPrice")
    private double unitPrice;

    public OrderLineBean() { /* no-arg constructor required by JPA */ }

    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
