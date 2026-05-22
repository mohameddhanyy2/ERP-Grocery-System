package com.groceryerp.supplier;

import com.groceryerp.interfaces.*;
import java.util.*;

/**
 * Supplier Module — Component implementation.
 *
 * PROVIDED interfaces: ISupplierService, IOrderStatus
 * REQUIRED interfaces: IStoreInventory, IStockAlerts (injected via IoC)
 *
 * EJB: annotate with @Stateless for session bean modelling.
 */
public class SupplierModule implements ISupplierService, IOrderStatus {

    // Required interfaces — injected via IoC
    private IStoreInventory storeInventory;
    private IStockAlerts stockAlerts;

    public SupplierModule() {}

    public void setStoreInventory(IStoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }
    public void setStockAlerts(IStockAlerts stockAlerts) {
        this.stockAlerts = stockAlerts;
    }

    @Override
    public String placeOrder(String supplierId, String productId, int quantity, String storeId) {
        // Check alert before placing order — requires IStockAlerts
        if (!stockAlerts.isRestockNeeded(productId, storeId)) {
            return "ORDER_SKIPPED: stock sufficient";
        }
        // TODO: Member 3 — create PurchaseOrderBean and persist
        return "ORDER-" + System.currentTimeMillis();
    }

    @Override
    public List<String> getAllSupplierIds() {
        // TODO: Member 3 — return supplier list
        return new ArrayList<>();
    }

    @Override
    public String getOrderStatus(String orderId) {
        // TODO: Member 3 — lookup order status
        return "PENDING";
    }

    @Override
    public double getTotalPurchaseCost(String period) {
        // TODO: Member 3 — sum costs for period
        return 0.0;
    }

    @Override
    public List<String> getOrderIdsByStore(String storeId) {
        // TODO: Member 3 — filter orders by store
        return new ArrayList<>();
    }
}
