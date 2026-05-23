package com.groceryerp.inventory;

// @Stateful
// Chosen because the store registry (List<IStoreInventory> stores) is conversational
// state: it is built up via repeated addStore() calls before any operations run,
// and every subsequent getTotalStock() / getStoresWithLowStock() call depends on
// that accumulated registry. The registry must persist across calls within the session.

import com.groceryerp.interfaces.IStockAlerts;
import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.interfaces.ITotalStock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CentralInventoryBean — Stateful Session Bean compositing all store branches.
 *
 * Session state: the List of IStoreInventory instances injected via addStore().
 * The registry accumulates through setup calls, then all query methods use it.
 * Implements ITotalStock and IStockAlerts — chain-wide inventory operations.
 *
 * Bean type: @Stateful — the store registry is built up across multiple addStore()
 * calls and must persist for the lifetime of this session.
 */
public class CentralInventoryBean implements ITotalStock, IStockAlerts, Serializable {

    // ── Session state — the store registry ───────────────────────
    private List<IStoreInventory> stores;

    public CentralInventoryBean() {
        this.stores = new ArrayList<>();
    }

    // ── IoC: stores injected, never created here ──────────────────

    /** Adds a store to the registry. Called during setup in Main.java. */
    public void addStore(IStoreInventory store) {
        stores.add(store);
    }

    /** Looks up a store by ID from the registry. */
    public IStoreInventory getStore(String storeId) {
        for (IStoreInventory store : stores) {
            if (store.getStoreId().equals(storeId)) { return store; }
        }
        return null;
    }

    // ── ITotalStock implementation ────────────────────────────────

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
            to.updateStock(productId, +qty);
        }
    }

    // ── IStockAlerts implementation ───────────────────────────────

    @Override
    public boolean isRestockNeeded(String productId, String storeId) {
        IStoreInventory store = getStore(storeId);
        if (store == null) { return false; }
        for (String alert : store.getLowStockAlerts()) {
            if (alert.contains(productId)) { return true; }
        }
        return false;
    }

    @Override
    public List<String> getProductsNeedingRestock(String storeId) {
        IStoreInventory store = getStore(storeId);
        return store != null ? store.getLowStockAlerts() : new ArrayList<>();
    }

    // ── @Remove — session end method ─────────────────────────────

    // @Remove
    /** Clears the store registry. Call when this central inventory session ends. */
    public void clearStores() {
        stores.clear();
    }

    // ── JavaBean accessors ────────────────────────────────────────
    public List<IStoreInventory> getStores() { return stores; }
    public void setStores(List<IStoreInventory> stores) { this.stores = stores; }
}
