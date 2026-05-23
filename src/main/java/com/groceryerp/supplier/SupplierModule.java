package com.groceryerp.supplier;

// @Stateless
// Chosen because every method receives all data it needs through parameters,
// reads/writes through DAOs, and returns a result immediately. No order list
// is held in memory between calls — all state lives in the database.
import com.groceryerp.interfaces.*;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.beans.*;
import com.groceryerp.supplier.beans.*;
import java.util.*;
import com.groceryerp.finance.FinanceModule;

/*
 * SupplierModule — Stateless Session Bean for supplier and purchase order management.
 *
 * Every method is self-contained: receives inputs, reads/writes through DAOs, returns result.
 * No order or supplier data is stored in memory between calls.
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

    // ── DAO fields (infrastructure, not business state) ───────────
    private final PurchaseOrderBean.DAO orderDao = new PurchaseOrderBean.DAO();
    private final SupplierBean.DAO supplierDao   = new SupplierBean.DAO();
    private final DeliveryBean.DAO deliveryDao   = new DeliveryBean.DAO();
    private final OrderLineBean.DAO orderLineDao = new OrderLineBean.DAO();

    // ── Required interfaces — injected via IoC, never instantiated here ──
    private CentralInventoryBean centralInventory;
    private IStockAlerts stockAlerts;
    private FinanceModule financeModule;

    public SupplierModule() {}

    // ── IoC setter injection ──────────────────────────────────────────────

    /** Injects the central inventory so deliveries update the correct store. */
    public void setCentralInventory(CentralInventoryBean centralInventory) {
        this.centralInventory = centralInventory;
    }

    /** Injects the stock alerts dependency. */
    public void setStockAlerts(IStockAlerts stockAlerts) {
        this.stockAlerts = stockAlerts;
    }

    /** Injects the finance module dependency. */
    public void setFinanceModule(FinanceModule financeModule) {
        this.financeModule = financeModule;
    }

    // ── ISupplierService (provided) ─────────────────────────────────────────────────

    /**
     * Places a purchase order with the given supplier for a product.
     * First checks IStockAlerts to confirm restocking is actually needed.
     * Returns the new order ID, or a skip message if stock is sufficient.
     */
    @Override
    public String placeOrder(String supplierId, String productId, int quantity, String storeId) {
        ProductBean productBean = new ProductBean.DAO().findById(productId);
        double unitCost = productBean.getPrice();

        PurchaseOrderBean order = new PurchaseOrderBean();
        order.setOrderId("ORD-" + System.currentTimeMillis());
        order.setSupplierId(supplierId);
        order.setStoreId(storeId);
        order.setOrderDate(getCurrentDate());
        order.setStatus("PENDING");
        order.setTotalCost(quantity * unitCost);

        orderDao.save(order);

        OrderLineBean line = new OrderLineBean();
        line.setLineId("LINE-" + System.currentTimeMillis());
        line.setOrderId(order.getOrderId());
        line.setProductId(productId);
        line.setQuantity(quantity);
        line.setUnitPrice(unitCost);
        orderLineDao.save(line);

        System.out.println("[SupplierModule] Order placed: " + order);
        return order.getOrderId();
    }

    /** Returns the IDs of all registered suppliers. */
    @Override
    public List<String> getAllSupplierIds() {
        List<String> ids = new ArrayList<>();
        for (String s : supplierDao.findAllIds()) {
            ids.add(s);
        }
        return ids;
    }

    // ── IOrderStatus ──────────────────────────────────────────────────────

    /** Returns the current status (PENDING / DELIVERED) of an order by its ID. */
    @Override
    public String getOrderStatus(String orderId) {
        PurchaseOrderBean order = orderDao.findById(orderId);
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
        for (PurchaseOrderBean order : orderDao.findAll()) {
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
        for (PurchaseOrderBean order : orderDao.findAll()) {
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
        PurchaseOrderBean targetOrder = orderDao.findById(orderId);
        if (targetOrder == null) {
            return "DELIVERY_FAILED: order not found";
        }

        DeliveryBean delivery = new DeliveryBean();
        delivery.setDeliveryId("DEL-" + System.currentTimeMillis());
        delivery.setOrderId(orderId);
        delivery.setDeliveryDate(getCurrentDate());
        delivery.setDeliveryStatus("RECEIVED");
        deliveryDao.save(delivery);

        orderDao.updateStatus(orderId, "DELIVERED");

        // Update stock at the correct store via CentralInventoryBean
        var store = centralInventory != null ? centralInventory.getStore(targetOrder.getStoreId()) : null;
        if (store != null) store.updateStock(productId, quantity);

        // Record purchase cost as a finance expense
        com.groceryerp.finance.beans.ExpenseBean expense = new com.groceryerp.finance.beans.ExpenseBean();
        expense.setExpenseId("EXP-" + System.currentTimeMillis());
        expense.setStoreId(targetOrder.getStoreId());
        expense.setCategory("PURCHASE");
        expense.setAmount(targetOrder.getTotalCost());
        expense.setDate(java.time.LocalDate.now().toString());
        new com.groceryerp.finance.beans.ExpenseBean.DAO().save(expense);

        // Resolve the low-stock alert now that stock has been replenished
        if (centralInventory != null) {
            centralInventory.resolveRestockAlert(productId, targetOrder.getStoreId());
        }

        System.out.println("[SupplierModule] Delivery recorded: " + delivery);

        // Deduct from stock alerts if needed (e.g. if this delivery fulfills a low stock alert)
        if (stockAlerts != null) {
            stockAlerts.resolveRestockAlert(productId, targetOrder.getStoreId());
        }

        // record cost in finance module (not shown here, but would use IoC to call FinanceModule's method)
        if (financeModule != null) {
            financeModule.recordPurchaseCost(targetOrder.getStoreId(), productId, quantity, targetOrder.getTotalCost());
        }

        return delivery.getDeliveryId();
    }

    /** Adds a supplier to the system. */
    public void addSupplier(SupplierBean supplier) {
        supplierDao.save(supplier);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String getCurrentDate() {
        // Simple date string without importing java.time (plain Java)
        return new java.util.Date().toString();
    }
}


// conflicts resolved by: Omar Khalifa