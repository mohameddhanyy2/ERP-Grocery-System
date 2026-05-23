package com.groceryerp.inventory;

// @MessageDriven
// Chosen because stock alerts are asynchronous events: StoreInventoryBean fires them
// after a stock update, and this bean reacts without the caller waiting for a result.
// It holds no conversational state — each onMessage() call is self-contained.

import com.groceryerp.interfaces.ISupplierService;
import com.groceryerp.inventory.beans.StockAlertBean;

/**
 * StockAlertMDB — Message-Driven Bean for handling low-stock alert messages.
 *
 * Triggered asynchronously when StoreInventoryBean.updateStock() detects that
 * a product quantity has dropped below the low-stock threshold. Persists the
 * alert and optionally places an automatic restock order via ISupplierService.
 *
 * Bean type: @MessageDriven — reacts to events, holds no conversational state,
 * never called directly by other modules.
 */
public class StockAlertMDB {

    // Required interface — injected via setter (IoC); may be null if not wired
    private ISupplierService supplierService;

    private final StockAlertBean.DAO alertDao = new StockAlertBean.DAO();

    public StockAlertMDB() {}

    /** Injects the supplier service for automatic restock order placement. */
    public void setSupplierService(ISupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * Entry point for incoming stock alert messages.
     *
     * @param messageType expected value: "LOW_STOCK"
     * @param payload     a StockAlertBean describing the product and store
     */
    public void onMessage(String messageType, Object payload) {
        if ("LOW_STOCK".equals(messageType)) {
            StockAlertBean alert = (StockAlertBean) payload;
            alertDao.save(alert);
            System.out.println("[MDB] Low stock alert received: product=" + alert.getProductId()
                    + " store=" + alert.getStoreId()
                    + " qty=" + alert.getCurrentQty()
                    + " threshold=" + alert.getThreshold());

            if (supplierService != null) {
                int restockQty = alert.getThreshold() * 5;
                String orderId = supplierService.placeOrder(
                        "SUP_001", alert.getProductId(), restockQty, alert.getStoreId());
                System.out.println("[MDB] Auto-restock order placed: " + orderId
                        + " for " + restockQty + " units of " + alert.getProductId());
            }
        }
    }
}
