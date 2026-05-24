package com.groceryerp.inventory;

// @MessageDriven
// Chosen because stock alerts are asynchronous events: StoreInventoryBean fires them
// after a stock update, and this bean reacts without the caller waiting for a result.
// It holds no conversational state — each onMessage() call is self-contained.

import com.groceryerp.api.AlertBroadcaster;
import com.groceryerp.inventory.beans.StockAlertBean;

/**
 * StockAlertMDB — Message-Driven Bean for handling low-stock alert messages.
 *
 * Triggered asynchronously when StoreInventoryBean.updateStock() detects that
 * a product quantity has dropped below the low-stock threshold. Persists the
 * alert so that suppliers can see it via the portal and initiate a quote.
 *
 * Bean type: @MessageDriven — reacts to events, holds no conversational state,
 * never called directly by other modules.
 */
public class StockAlertMDB {

    private final StockAlertBean.DAO alertDao = new StockAlertBean.DAO();

    public StockAlertMDB() {}

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
            AlertBroadcaster.getInstance().broadcast(alert.getProductId());
            System.out.println("[MDB] Low stock alert saved: product=" + alert.getProductId()
                    + " store=" + alert.getStoreId()
                    + " qty=" + alert.getCurrentQty()
                    + " threshold=" + alert.getThreshold()
                    + " — awaiting supplier quote");
        }
    }
}
