package com.groceryerp.inventory;

import com.groceryerp.interfaces.IStockAlerts;
import com.groceryerp.interfaces.IStoreInventory;
import com.groceryerp.interfaces.ITotalStock;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Composite component of the inventory module — aggregates every
 * {@link StoreInventoryBean} branch into a single chain-wide view.
 * <p>
 * Now a real @Stateful session bean (previously a plain object). Chosen because
 * the store registry ({@code List<IStoreInventory> stores}) is conversational
 * state: it is built up via repeated {@link #addStore(IStoreInventory)} calls
 * before any operations run, and every subsequent getTotalStock() /
 * getStoresWithLowStock() call depends on that accumulated registry. The
 * registry must persist across calls within the session — so this is @Stateful,
 * NOT a JPA entity.
 * <p>
 * <b>PROVIDED interfaces:</b> {@link ITotalStock}, {@link IStockAlerts}<br>
 * <b>REQUIRED:</b> the @Stateless {@link InventoryRepository}, injected by the
 * container (replaces the old {@code new StockAlertBean.DAO()} usage).
 */
@Stateful
@LocalBean  // expose the concrete no-interface view so @EJB CentralInventoryBean can bind
            // (resources call addStore()/getStore(), which are on the class, not on
            // ITotalStock/IStockAlerts). Without this, EJB resolves the bean only by
            // its interfaces and the by-class @EJB injection fails (WFLYEJB0406).
public class CentralInventoryBean implements ITotalStock, IStockAlerts, Serializable {

    /** Child stores held by interface, not by concrete class (Composite). */
    private List<IStoreInventory> stores;

    /** Container-injected persistence service (replaces the StockAlertBean.DAO). */
    @EJB
    private InventoryRepository repository;

    /**
     * CDI provider for store branches. Using Instance (not {@code new}) means each
     * StoreInventoryBean we obtain is container-managed, so its @Inject
     * InventoryRepository / StockAlertProducer are populated — a plain
     * {@code new StoreInventoryBean()} would leave those null.
     */
    @Inject
    private Instance<StoreInventoryBean> storeProvider;

    /** Public no-argument constructor required by the JavaBeans / EJB spec. */
    public CentralInventoryBean() {
        this.stores = new ArrayList<>();
    }

    /**
     * Load every store that already exists in the DB into the composite registry.
     * The original Main.java did this wiring at startup by calling addStore() for
     * each branch; that bootstrap was removed in the EJB migration, leaving the
     * registry empty so processSale() failed with "Unknown store". This rebuilds it
     * lazily when the @Stateful bean is created.
     */
    @PostConstruct
    public void loadStoresFromDb() {
        for (Map<String, String> row : repository.loadStores()) {
            StoreInventoryBean store = storeProvider.get();
            store.setStoreId(row.get("storeId"));
            store.setStoreName(row.get("storeName"));
            stores.add(store);
        }
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
     * Finds a store by id. Checks the in-memory registry first; if absent (this
     * @Stateful instance was created before the store existed, or the store was
     * added elsewhere), it falls back to the DB and lazily registers a
     * container-managed StoreInventoryBean. Returns null only if the store truly
     * does not exist. This keeps every caller (POS, restock, delivery, transfer)
     * working regardless of when this bean instance was constructed.
     *
     * @param storeId the branch code to look for.
     * @return the matching store, or {@code null} if none exists.
     */
    public IStoreInventory getStore(String storeId) {
        for (IStoreInventory store : stores) {
            if (store.getStoreId().equals(storeId)) { return store; }
        }
        // Not in the in-memory registry — check the DB and register it if present.
        for (Map<String, String> row : repository.loadStores()) {
            if (row.get("storeId").equals(storeId)) {
                StoreInventoryBean store = storeProvider.get();
                store.setStoreId(row.get("storeId"));
                store.setStoreName(row.get("storeName"));
                stores.add(store);
                return store;
            }
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

    @Override
    public void resolveRestockAlert(String productId, String storeId) {
        IStoreInventory store = getStore(storeId);
        if (store != null) {
            store.getLowStockAlerts().removeIf(alert -> alert.contains(productId));
        }
        // Was: new StockAlertBean.DAO().deleteByProductAndStore(productId, storeId);
        repository.deleteByProductAndStore(productId, storeId);
    }

    // ── @Remove — session end method ─────────────────────────────

    @Remove
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
