package com.groceryerp.inventory;

import com.groceryerp.interfaces.*;
import java.io.Serializable;
import java.util.*;

/**
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
            if (store.getStoreId().equals(storeId)) return store;
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
            to.updateStock(productId,  +qty);
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
        return store != null && store.getLowStockAlerts().contains(productId);
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

    // ── JavaBean accessors ──────────────────────────────────────────

    /** @return the list of injected child stores. */
    public List<IStoreInventory> getStores() { return stores; }
    /** @param stores the list of child stores to set. */
    public void setStores(List<IStoreInventory> stores) { this.stores = stores; }

    /**
     * Standalone demo proving the composite's provided interfaces
     * ({@link ITotalStock}, {@link IStockAlerts}) work in isolation.
     * Run with:
     * <pre>java com.groceryerp.inventory.CentralInventoryBean</pre>
     */
    public static void main(String[] args) {
        System.out.println("=== CentralInventoryBean — Composite Structure demo ===\n");

        // Build three store branches (the leaf components).
        StoreInventoryBean a = new StoreInventoryBean();
        a.setStoreId("STORE_A");
        a.setStoreName("Downtown Branch");
        a.updateStock("PROD_001", 60);
        a.updateStock("PROD_002", 3);   // below threshold -> low stock

        StoreInventoryBean b = new StoreInventoryBean();
        b.setStoreId("STORE_B");
        b.setStoreName("Uptown Branch");
        b.updateStock("PROD_001", 40);

        StoreInventoryBean c = new StoreInventoryBean();
        c.setStoreId("STORE_C");
        c.setStoreName("Harbor Branch");
        c.updateStock("PROD_001", 5);   // below threshold -> low stock

        // IoC: stores are INJECTED into the composite, never created inside it.
        CentralInventoryBean central = new CentralInventoryBean();
        central.addStore(a);
        central.addStore(b);
        central.addStore(c);
        System.out.println("Composite assembled with " + central.getStores().size() + " stores.\n");

        // ITotalStock — chain-wide operations
        System.out.println("getTotalStock(PROD_001)  -> " + central.getTotalStock("PROD_001"));
        System.out.println("getStoresWithLowStock()  -> " + central.getStoresWithLowStock());

        // redistributeStock — move 20 units from STORE_A to STORE_C
        central.redistributeStock("STORE_A", "STORE_C", "PROD_001", 20);
        System.out.println("\nAfter redistributing 20x PROD_001 from STORE_A to STORE_C:");
        System.out.println("  STORE_A PROD_001       -> " + central.getStore("STORE_A").checkStock("PROD_001"));
        System.out.println("  STORE_C PROD_001       -> " + central.getStore("STORE_C").checkStock("PROD_001"));
        System.out.println("  getTotalStock(PROD_001)-> " + central.getTotalStock("PROD_001") + "  (unchanged)");

        // IStockAlerts
        System.out.println("\nisRestockNeeded(PROD_002, STORE_A) -> "
                + central.isRestockNeeded("PROD_002", "STORE_A"));
        System.out.println("getProductsNeedingRestock(STORE_A) -> "
                + central.getProductsNeedingRestock("STORE_A"));

        boolean ok = central.getTotalStock("PROD_001") == 105
                && central.getStoresWithLowStock().contains("STORE_A")
                && central.isRestockNeeded("PROD_002", "STORE_A");
        System.out.println("\nComposite demo: " + (ok ? "PASS" : "FAIL"));
    }
}
