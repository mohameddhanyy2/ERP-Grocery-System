package com.groceryerp.supplier.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * DeliveryBean — JPA entity mapped to the {@code deliveries} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT / UPDATE). The DAO is gone —
 * persistence is now handled by {@link com.groceryerp.supplier.SupplierRepository}
 * via the container-managed EntityManager. The annotations below ARE the mapping.
 */
@Entity
@Table(name = "deliveries")
public class DeliveryBean implements Serializable {

    @Id
    @Column(name = "deliveryId")
    private String deliveryId;

    @Column(name = "orderId")
    private String orderId;

    @Column(name = "deliveryDate")
    private String deliveryDate;

    @Column(name = "deliveryStatus")
    private String deliveryStatus;

    public DeliveryBean() { /* no-arg constructor required by JPA */ }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}
