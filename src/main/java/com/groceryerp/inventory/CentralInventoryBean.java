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

/*
 * Composite component of the inventory module — aggregates every
 * {@link StoreInventoryBean} branch into a single chain-wide view.
 * <p>
 * <b>PROVIDED interfaces:</b> {@link ITotalStock}, {@link IStockAlerts}<br>
 * <b>REQUIRED interfaces:</b> none — this is a foundation component.
 * <p>
 * <b>Composite Structure:</b> the bean holds a {@code List<IStoreInventory>}
 * and exposes operations over the whole collection. Because it works through
 * the {@link IStoreInventory} interface, a caller cannot tell whether it is
 * talking to one store or to the entire chain.
 * <p>
 * <b>Inversion of Control:</b> stores are <i>injected</i> from outside via
 * {@link #addStore(IStoreInventory)} — the composite never creates a store
 * with {@code new}.
 */
public class CentralInventoryBean implements ITotalStock, IStockAlerts, Serializable {

    /** Child stores held by interface, not by concrete class (Composite). */
    private List<IStoreInventory> stores;

    /** Public no-argument constructor required by the JavaBeans spec. */
    public CentralInventoryBean() {
        this.stores = new ArrayList<>();
    }

    // ── IoC: stores injected, not created ──────────────────────────

    /**
     * Injects one store branch into the composite (Inversion of Control).
     *
     * @param store the store to add, supplied from outside.
     */
    public void addStore(IStoreInventory store) {
        stores.add(store);
    }

    /**
     * Finds an injected store by its id.
     *
     * @param storeId the branch code to look for.
     * @return the matching store, or {@code null} if none matches.
     */
    public IStoreInventory getStore(String storeId) {
        for (IStoreInventory store : stores) {
            if (store.getStoreId().equals(storeId)) { return store; }
        }
        return null;
    }

    // ── ITotalStock implementation ──────────────────────────────────

    /**
     * Adds up the stock of one product across every branch.
     *
     * @param productId the product code to total.
     * @return the chain-wide quantity on hand.
     */
    @Override
    public int getTotalStock(String productId) {
        int total = 0;
        for (IStoreInventory store : stores) {
            total += store.checkStock(productId);
        }
        return total;
    }

    /**
     * Lists every branch that currently has at least one low-stock product.
     *
     * @return the ids of stores reporting low stock.
     */
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

    /**
     * Moves stock of a product from one branch to another. Does nothing
     * if either branch id is unknown.
     *
     * @param fromStoreId the branch to take stock from.
     * @param toStoreId   the branch to give stock to.
     * @param productId   the product to move.
     * @param qty         the number of units to move.
     */
    @Override
    public void redistributeStock(String fromStoreId, String toStoreId, String productId, int qty) {
        IStoreInventory from = getStore(fromStoreId);
        IStoreInventory to   = getStore(toStoreId);
        if (from != null && to != null) {
            from.updateStock(productId, -qty);
            to.updateStock(productId, +qty);
        }
    }

    // ── IStockAlerts implementation ─────────────────────────────────

    /**
     * Tells whether a given product needs restocking in a given branch.
     *
     * @param productId the product to check.
     * @param storeId   the branch to check.
     * @return {@code true} if the product is low at that branch.
     */
    @Override
    public boolean isRestockNeeded(String productId, String storeId) {
        IStoreInventory store = getStore(storeId);
        if (store == null) { return false; }
        for (String alert : store.getLowStockAlerts()) {
            if (alert.contains(productId)) { return true; }
        }
        return false;
    }

    /**
     * Lists every product that needs restocking in a given branch.
     *
     * @param storeId the branch to check.
     * @return the low-stock product codes, or an empty list if the branch
     *         is unknown.
     */
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

    // ── JavaBean accessors ──────────────────────────────────────────

    /** @return the list of injected child stores. */
    public List<IStoreInventory> getStores() { return stores; }

    /** @param stores the list of child stores to set. */
    public void setStores(List<IStoreInventory> stores) { this.stores = stores; }

}

// conflicts resolved by: Omar Khalifa