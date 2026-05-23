package com.groceryerp.supplier;

import com.groceryerp.interfaces.*;
import com.groceryerp.supplier.beans.*;
import java.util.*;

/**
 * Supplier Module — Component implementation.
 *
 * EJB Session Type: @Stateless
 *   Reason: Each method call (placeOrder, getOrderStatus) is self-contained.
 *   No conversational state needs to be held between calls, so Stateless fits.
 *   Contrast with @Stateful (used by HRModule) where payroll spans multiple steps.
 *
 * PROVIDED interfaces : ISupplierService, IOrderStatus
 * REQUIRED interfaces : IStoreInventory, IStockAlerts  (injected via IoC setters)
 *
 * IoC rule: this class never calls "new StoreInventoryBean()" or any other module.
 *           All dependencies arrive from outside through setter injection (Main.java).
 */
// @Stateless  -- EJB annotation (requires Jakarta EE runtime; shown here for modelling)
public class SupplierModule implements ISupplierService, IOrderStatus {

    // ── Required interfaces — injected via IoC, never instantiated here ──
    private IStoreInventory storeInventory;
    private IStockAlerts stockAlerts;

    // ── In-memory storage (plain Java — no database) ──
    private final List<SupplierBean> suppliers = new ArrayList<>();
    private final List<PurchaseOrderBean> orders = new ArrayList<>();
    private final List<DeliveryBean> deliveries = new ArrayList<>();
    private int orderCounter = 1;
    private int deliveryCounter = 1;

    public SupplierModule() {
        seedSuppliers();
    }

    // ── IoC setter injection ──────────────────────────────────────────────

    public void setStoreInventory(IStoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }

    public void setStockAlerts(IStockAlerts stockAlerts) {
        this.stockAlerts = stockAlerts;
    }

    // ── ISupplierService ─────────────────────────────────────────────────

    /**
     * Places a purchase order with the given supplier for a product.
     * First checks IStockAlerts to confirm restocking is actually needed.
     * Returns the new order ID, or a skip message if stock is sufficient.
     */
    @Override
    public String placeOrder(String supplierId, String productId, int quantity, String storeId) {
        if (!stockAlerts.isRestockNeeded(productId, storeId)) {
            return "ORDER_SKIPPED: stock sufficient for " + productId + " at " + storeId;
        }

        String orderId = "ORD-" + orderCounter++;

        PurchaseOrderBean order = new PurchaseOrderBean();
        order.setOrderId(orderId);
        order.setSupplierId(supplierId);
        order.setStoreId(storeId);
        order.setOrderDate(getCurrentDate());
        order.setStatus("PENDING");
        order.setTotalCost(quantity * 5.0);  // default unit cost: 5.0 per unit

        orders.add(order);

        System.out.println("[SupplierModule] Order placed: " + order);
        return orderId;
    }

    /** Returns the IDs of all registered suppliers. */
    @Override
    public List<String> getAllSupplierIds() {
        List<String> ids = new ArrayList<>();
        for (SupplierBean s : suppliers) {
            ids.add(s.getSupplierId());
        }
        return ids;
    }

    // ── IOrderStatus ──────────────────────────────────────────────────────

    /** Returns the current status (PENDING / DELIVERED) of an order by its ID. */
    @Override
    public String getOrderStatus(String orderId) {
        for (PurchaseOrderBean order : orders) {
            if (order.getOrderId().equals(orderId)) {
                return order.getStatus();
            }
        }
        return "NOT_FOUND";
    }

    /**
     * Sums the total cost of all orders whose orderDate contains the given period.
     * Period format: "YYYY-MM"  e.g. "2025-05"
     */
    @Override
    public double getTotalPurchaseCost(String period) {
        double total = 0.0;
        for (PurchaseOrderBean order : orders) {
            if (order.getOrderDate().contains(period)) {
                total += order.getTotalCost();
            }
        }
        return total;
    }

    /** Returns all order IDs that were placed for the given store. */
    @Override
    public List<String> getOrderIdsByStore(String storeId) {
        List<String> result = new ArrayList<>();
        for (PurchaseOrderBean order : orders) {
            if (order.getStoreId().equals(storeId)) {
                result.add(order.getOrderId());
            }
        }
        return result;
    }

    // ── Extra helper: record a delivery and restock the store ─────────────

    /**
     * Records arrival of goods for an order and restocks the store inventory.
     * Marks the order as DELIVERED.
     * Uses IStoreInventory — the required interface — to update stock.
     */
    public String recordDelivery(String orderId, String productId, int quantity) {
        PurchaseOrderBean targetOrder = null;
        for (PurchaseOrderBean order : orders) {
            if (order.getOrderId().equals(orderId)) {
                targetOrder = order;
                break;
            }
        }
        if (targetOrder == null) {
            return "DELIVERY_FAILED: order not found";
        }

        String deliveryId = "DEL-" + deliveryCounter++;

        DeliveryBean delivery = new DeliveryBean();
        delivery.setDeliveryId(deliveryId);
        delivery.setOrderId(orderId);
        delivery.setDeliveryDate(getCurrentDate());
        delivery.setDeliveryStatus("RECEIVED");
        deliveries.add(delivery);

        targetOrder.setStatus("DELIVERED");

        // Update stock via required IStoreInventory interface (IoC — never new StoreInventoryBean)
        storeInventory.updateStock(productId, quantity);

        System.out.println("[SupplierModule] Delivery recorded: " + delivery);
        return deliveryId;
    }

    /** Adds a supplier to the system. */
    public void addSupplier(SupplierBean supplier) {
        suppliers.add(supplier);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String getCurrentDate() {
        // Simple date string without importing java.time (plain Java)
        return new java.util.Date().toString();
    }

    /** Pre-loads two demo suppliers so getAllSupplierIds() returns real data. */
    private void seedSuppliers() {
        SupplierBean s1 = new SupplierBean();
        s1.setSupplierId("SUP-001");
        s1.setName("FreshFarm Co.");
        s1.setContactEmail("orders@freshfarm.com");
        s1.setLeadTimeDays(3);
        suppliers.add(s1);

        SupplierBean s2 = new SupplierBean();
        s2.setSupplierId("SUP-002");
        s2.setName("MegaWholesale Ltd.");
        s2.setContactEmail("supply@megaw.com");
        s2.setLeadTimeDays(7);
        suppliers.add(s2);
    }
}
