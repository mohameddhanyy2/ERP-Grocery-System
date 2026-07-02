package com.groceryerp.supplier;

import com.groceryerp.finance.FinanceModule;
import com.groceryerp.interfaces.IOrderStatus;
import com.groceryerp.interfaces.IStockAlerts;
import com.groceryerp.interfaces.ISupplierService;
import com.groceryerp.inventory.beans.ProductBean;
import com.groceryerp.supplier.beans.DeliveryBean;
import com.groceryerp.supplier.beans.OrderLineBean;
import com.groceryerp.supplier.beans.PurchaseOrderBean;
import com.groceryerp.supplier.beans.SupplierBean;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * SupplierModule — now a real @Stateless session bean (previously a plain object
 * manually instantiated and IoC-wired in Main.java via setter injection).
 *
 * EJB Session Type: @Stateless
 *   Each call (placeOrder, recordDelivery, getOrderStatus) is self-contained; no
 *   conversational state is held between calls — all state lives in the database.
 *
 * PROVIDED interfaces : ISupplierService, IOrderStatus
 * REQUIRED interfaces : ITotalStock, IStockAlerts — injected by the container via
 *                       @Inject, replacing the old setCentralInventory() /
 *                       setStockAlerts() setter calls in Main.java. Both are
 *                       produced by the composite CentralInventoryBean.
 *
 * Persistence is delegated to the injected @Stateless {@link SupplierRepository}
 * (the old PurchaseOrderBean.DAO / SupplierBean.DAO / DeliveryBean.DAO /
 * OrderLineBean.DAO fields).
 */
@Stateless
@LocalBean
public class SupplierModule implements ISupplierService, IOrderStatus {

    /** Container-injected persistence service (replaces the 4 nested DAO fields). */
    @EJB
    private SupplierRepository repository;

    /**
     * Inventory persistence. A delivery replenishes stock by writing directly to
     * the stock table via adjustStock() — this is reliable regardless of whether
     * the @Stateful CentralInventoryBean's in-memory store registry happens to
     * contain the target store in this bean instance.
     */
    @EJB
    private com.groceryerp.inventory.InventoryRepository inventoryRepository;

    /**
     * Required: low-stock alert service. Used to clear a restock alert once a
     * delivery replenishes the product. Was set via setStockAlerts() in Main.java.
     */
    @Inject
    private IStockAlerts stockAlerts;

    /** Required cross-module dependency for booking the purchase cost as an expense. */
    @Inject
    private FinanceModule financeModule;

    public SupplierModule() { /* required no-arg constructor for the container */ }

    // ── ISupplierService (provided) ───────────────────────────────

    /**
     * Places a purchase order with the given supplier for a product.
     * Returns the new order ID.
     */
    @Override
    public String placeOrder(String supplierId, String productId, int quantity, String storeId) {
        ProductBean productBean = repository.findProductById(productId);
        double unitCost = productBean.getUnitCost();

        PurchaseOrderBean order = new PurchaseOrderBean();
        order.setOrderId("ORD-" + System.currentTimeMillis());
        order.setSupplierId(supplierId);
        order.setStoreId(storeId);
        order.setOrderDate(getCurrentDate());
        order.setStatus("ACCEPTED");
        order.setTotalCost(quantity * unitCost);

        repository.savePurchaseOrder(order);

        OrderLineBean line = new OrderLineBean();
        line.setLineId("LINE-" + System.currentTimeMillis());
        line.setOrderId(order.getOrderId());
        line.setProductId(productId);
        line.setQuantity(quantity);
        line.setUnitPrice(unitCost);
        repository.saveOrderLine(line);

        System.out.println("[SupplierModule] Order placed: " + order);
        return order.getOrderId();
    }

    /** Returns the IDs of all registered suppliers. */
    @Override
    public List<String> getAllSupplierIds() {
        List<String> ids = new ArrayList<>();
        for (String s : repository.findAllSupplierIds()) {
            ids.add(s);
        }
        return ids;
    }

    // ── IOrderStatus (provided) ───────────────────────────────────

    /** Returns the current status (PENDING / DELIVERED) of an order by its ID. */
    @Override
    public String getOrderStatus(String orderId) {
        PurchaseOrderBean order = repository.findPurchaseOrderById(orderId);
        if (order != null) {
            return order.getStatus();
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
        for (PurchaseOrderBean order : repository.findAllPurchaseOrders()) {
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
        for (PurchaseOrderBean order : repository.findAllPurchaseOrders()) {
            if (order.getStoreId().equals(storeId)) {
                result.add(order.getOrderId());
            }
        }
        return result;
    }

    // ── Extra helper: record a delivery and restock the store ─────

    /**
     * Records arrival of goods for an order and restocks the store inventory.
     * Marks the order as DELIVERED. Uses the injected inventory (ITotalStock) to
     * locate the store and the injected IStockAlerts to clear the restock alert.
     */
    public String recordDelivery(String orderId, String productId, int quantity) {
        PurchaseOrderBean targetOrder = repository.findPurchaseOrderById(orderId);
        if (targetOrder == null) {
            return "DELIVERY_FAILED: order not found";
        }

        // Always adjust stock for this line — the frontend calls once per order line,
        // each with a different productId, so all lines must update stock.
        inventoryRepository.adjustStock(targetOrder.getStoreId(), productId, quantity);

        // Mark order DELIVERED and create the delivery record only on the first call
        // (when it is not yet DELIVERED). Subsequent line calls skip this so we don't
        // create duplicate delivery records or double-book the expense.
        if (!"DELIVERED".equals(targetOrder.getStatus())) {
            DeliveryBean delivery = new DeliveryBean();
            delivery.setDeliveryId("DEL-" + System.currentTimeMillis());
            delivery.setOrderId(orderId);
            delivery.setDeliveryDate(getCurrentDate());
            delivery.setDeliveryStatus("RECEIVED");
            repository.saveDelivery(delivery);
            repository.updateOrderStatus(orderId, "DELIVERED");

            if (financeModule != null) {
                financeModule.recordPurchaseCost(targetOrder.getStoreId(), productId, quantity, targetOrder.getTotalCost());
            }
        }

        if (stockAlerts != null) {
            stockAlerts.resolveRestockAlert(productId, targetOrder.getStoreId());
        }

        return "DEL-" + orderId;
    }

    /** Adds a supplier to the system. */
    public void addSupplier(SupplierBean supplier) {
        repository.saveSupplier(supplier);
    }

    // ── Private helpers ───────────────────────────────────────────

    private String getCurrentDate() {
        // Simple date string (preserves the original plain-Java behaviour).
        return new java.util.Date().toString();
    }
}
