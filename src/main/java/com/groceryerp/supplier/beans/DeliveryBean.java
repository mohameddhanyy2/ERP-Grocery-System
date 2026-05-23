package com.groceryerp.supplier.beans;

import java.io.Serializable;

/**
 * JavaBean — records the actual arrival of goods from a supplier.
 * Created when a delivery is confirmed against a PurchaseOrderBean.
 */
public class DeliveryBean implements Serializable {

    private String deliveryId;
    private String orderId;
    private String deliveryDate;
    private String deliveryStatus;  // RECEIVED | PENDING

    public DeliveryBean() {}

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    @Override
    public String toString() {
        return "DeliveryBean{deliveryId='" + deliveryId + "', orderId='" + orderId +
               "', deliveryDate='" + deliveryDate + "', deliveryStatus='" + deliveryStatus + "'}";
    }
}
