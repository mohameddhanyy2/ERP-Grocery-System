package com.groceryerp.supplier;

// @Stateless
// Chosen because every method receives all data it needs through parameters,
// reads/writes through DAOs, and returns a result immediately. No order list
// is held in memory between calls — all state lives in the database.

import com.groceryerp.interfaces.IOrderStatus;
import com.groceryerp.interfaces.IStockAlerts;
import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.interfaces.ISupplierService;
import com.groceryerp.supplier.beans.PurchaseOrderBean;
import com.groceryerp.supplier.beans.SupplierBean;

import java.time.LocalDate;
import java.util.List;

/**
 * SupplierModule — Stateless Session Bean for supplier and purchase order management.
 *
 * Every method is self-contained: receives inputs, reads/writes through DAOs, returns result.
 * No order or supplier data is stored in memory between calls.
 *
 * PROVIDED interfaces: ISupplierService, IOrderStatus
 * REQUIRED interfaces: IStoreInventory, IStockAlerts (injected via IoC setters)
 *
 * Bean type: @Stateless — no conversational state, all data flows through parameters and DAOs.
 */
public class SupplierModule implements ISupplierService, IOrderStatus {

    // ── Injected interfaces (infrastructure, not business state) ──
    private IStoreInventory storeInventory;
    private IStockAlerts stockAlerts;

    // ── DAO fields (infrastructure, not business state) ───────────
    private final PurchaseOrderBean.DAO orderDao = new PurchaseOrderBean.DAO();
    private final SupplierBean.DAO supplierDao   = new SupplierBean.DAO();

    public SupplierModule() { /* no-arg constructor required by IoC */ }

    /** Injects the store inventory dependency. */
    public void setStoreInventory(IStoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }

    /** Injects the stock alerts dependency. */
    public void setStockAlerts(IStockAlerts stockAlerts) {
        this.stockAlerts = stockAlerts;
    }

    // ── ISupplierService (provided) ───────────────────────────────

    /**
     * Places a purchase order with a supplier and persists it.
     * Returns ORDER_SKIPPED if stock is already sufficient.
     */
    @Override
    public String placeOrder(String supplierId, String productId, int quantity, String storeId) {
        if (stockAlerts != null && !stockAlerts.isRestockNeeded(productId, storeId)) {
            return "ORDER_SKIPPED: stock sufficient";
        }

        PurchaseOrderBean order = new PurchaseOrderBean();
        order.setOrderId("ORD-" + System.currentTimeMillis());
        order.setSupplierId(supplierId);
        order.setStoreId(storeId);
        order.setOrderDate(LocalDate.now().toString());
        order.setTotalCost(0.0);
        order.setStatus("PENDING");
        orderDao.save(order);

        return order.getOrderId();
    }

    /** Returns all supplier IDs from the database. */
    @Override
    public List<String> getAllSupplierIds() {
        return supplierDao.findAllIds();
    }

    // ── IOrderStatus (provided) ───────────────────────────────────

    /** Returns the status of the given order, or "NOT_FOUND" if it does not exist. */
    @Override
    public String getOrderStatus(String orderId) {
        PurchaseOrderBean order = orderDao.findById(orderId);
        return order != null ? order.getStatus() : "NOT_FOUND";
    }

    /** Returns the sum of totalCost for all orders placed in the given period. */
    @Override
    public double getTotalPurchaseCost(String period) {
        return orderDao.sumCostByPeriod(period);
    }

    /** Returns all order IDs for the given store. */
    @Override
    public List<String> getOrderIdsByStore(String storeId) {
        return orderDao.findIdsByStore(storeId);
    }
}
