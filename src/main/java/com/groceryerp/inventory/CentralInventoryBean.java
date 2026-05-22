package com.groceryerp.inventory;

import com.groceryerp.interfaces.*;
import java.io.Serializable;
import java.util.*;

/**
 * Composite component — aggregates all StoreInventoryBean instances.
 *
 * PROVIDED interfaces: IStoreInventory, ITotalStock, IStockAlerts
 * REQUIRED interfaces: none
 *
 * IoC: stores are INJECTED via addStore() — never instantiated here.
 * This is the Composite Structure pattern: holds a List<IStoreInventory>
 * and exposes chain-wide operations over all stores.
 */
public class CentralInventoryBean implements ITotalStock, IStockAlerts, Serializable {

    // Composite holds components by INTERFACE, not by concrete class
    private List<IStoreInventory> stores;

    public CentralInventoryBean() {
        this.stores = new ArrayList<>();
    }

    // ── IoC: stores injected, not created ──────────────────────────
    public void addStore(IStoreInventory store) {
        stores.add(store);
    }

    public IStoreInventory getStore(String storeId) {
        for (IStoreInventory store : stores) {
            if (store.getStoreId().equals(storeId)) return store;
        }
        return null;
    }

    // ── ITotalStock implementation ──────────────────────────────────
    @Override
    public int getTotalStock(String productId) {
        int total = 0;
        for (IStoreInventory store : stores) {
            total += store.checkStock(productId);
        }
        return total;
    }

    @Override
    public List<String> getStoresWithLowStock() {
        List<String> result = new ArrayList<>();
        for (IStoreInventory store : stores) {
            if (!store.getLowStockAlerts().isEmpty()) {
                result.add(store.getStoreId());
            }
        }
        return result;
    }

    @Override
    public void redistributeStock(String fromStoreId, String toStoreId, String productId, int qty) {
        IStoreInventory from = getStore(fromStoreId);
        IStoreInventory to   = getStore(toStoreId);
        if (from != null && to != null) {
            from.updateStock(productId, -qty);
            to.updateStock(productId,  +qty);
        }
    }

    // ── IStockAlerts implementation ─────────────────────────────────
    @Override
    public boolean isRestockNeeded(String productId, String storeId) {
        IStoreInventory store = getStore(storeId);
        return store != null && store.getLowStockAlerts().contains(productId);
    }

    @Override
    public List<String> getProductsNeedingRestock(String storeId) {
        IStoreInventory store = getStore(storeId);
        return store != null ? store.getLowStockAlerts() : new ArrayList<>();
    }

    // ── JavaBean accessors ──────────────────────────────────────────
    public List<IStoreInventory> getStores() { return stores; }
    public void setStores(List<IStoreInventory> stores) { this.stores = stores; }
}
