package com.groceryerp.inventory;

import com.groceryerp.inventory.beans.ProductBean;
import com.groceryerp.inventory.beans.StockAlertBean;
import com.groceryerp.inventory.beans.StockBean;
import com.groceryerp.inventory.beans.StockId;
import com.groceryerp.inventory.beans.StoreBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * InventoryRepository — @Stateless session bean that owns all persistence for the
 * inventory domain. Replaces the two nested DAO classes that used to live inside
 * {@link ProductBean} and {@link StockAlertBean}.
 *
 * The container injects a transaction-scoped {@link EntityManager} via
 * {@code @PersistenceContext}; each business method runs in a container-managed
 * JTA transaction (the EJB default is REQUIRED), so there is no manual
 * Connection/commit/close as there was with JDBC.
 */
@Stateless
public class InventoryRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── ProductBean.DAO replacement ───────────────────────────────

    /**
     * Replaces ProductBean.DAO.save().
     * Original SQL:
     *   INSERT OR REPLACE INTO products (productId,name,category,price,expiryDate) VALUES (?,?,?,?,?)
     * em.merge() emits the dialect-correct upsert.
     */
    public void saveProduct(ProductBean product) {
        em.merge(product);
    }

    /**
     * Replaces ProductBean.DAO.findById().
     * Original SQL:
     *   SELECT productId,name,category,price,expiryDate FROM products WHERE productId = ?
     */
    public ProductBean findProductById(String productId) {
        return em.find(ProductBean.class, productId);
    }

    /**
     * Replaces ProductBean.DAO.findAll().
     * Original SQL:
     *   SELECT productId,name,category,price,expiryDate FROM products
     */
    public List<ProductBean> findAllProducts() {
        return em.createQuery(
                "SELECT p FROM ProductBean p", ProductBean.class)
                .getResultList();
    }

    /**
     * Backs ProductServlet's old "SELECT * FROM products ORDER BY category, name".
     */
    public List<ProductBean> findAllProductsOrdered() {
        return em.createQuery(
                "SELECT p FROM ProductBean p ORDER BY p.category, p.name", ProductBean.class)
                .getResultList();
    }

    // ── StockAlertBean.DAO replacement ────────────────────────────

    /**
     * Replaces StockAlertBean.DAO.save().
     * Original SQL:
     *   INSERT OR REPLACE INTO stock_alerts (alertId,productId,storeId,currentQty,threshold,alertDate)
     *   VALUES (?,?,?,?,?,?)
     */
    public void saveAlert(StockAlertBean alert) {
        em.merge(alert);
    }

    /**
     * Replaces StockAlertBean.DAO.findByStore().
     * Original SQL:
     *   SELECT alertId,productId,storeId,currentQty,threshold,alertDate
     *   FROM stock_alerts WHERE storeId = ?
     */
    public List<StockAlertBean> findAlertsByStore(String storeId) {
        return em.createQuery(
                "SELECT a FROM StockAlertBean a WHERE a.storeId = :storeId", StockAlertBean.class)
                .setParameter("storeId", storeId)
                .getResultList();
    }

    /**
     * Replaces StockAlertBean.DAO.deleteByProductAndStore().
     * Original SQL:
     *   DELETE FROM stock_alerts WHERE productId = ? AND storeId = ?
     * Ported as a bulk JPQL DELETE executed via executeUpdate().
     */
    public int deleteByProductAndStore(String productId, String storeId) {
        return em.createQuery(
                "DELETE FROM StockAlertBean a WHERE a.productId = :productId AND a.storeId = :storeId")
                .setParameter("productId", productId)
                .setParameter("storeId", storeId)
                .executeUpdate();
    }

    /**
     * Product IDs at a store whose stock is below the threshold. JPQL over the
     * {@link StockBean} entity (was a native query on the un-mapped stock table).
     */
    public List<String> findLowStockProductIds(String storeId, int threshold) {
        return em.createQuery(
                "SELECT s.productId FROM StockBean s WHERE s.storeId = :storeId AND s.quantity < :threshold",
                String.class)
                .setParameter("storeId", storeId)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    // ── stock-table access (now via the StockBean entity) ─

    /**
     * Returns the on-hand quantity of a product at a store, or 0 if unknown.
     */
    public int getStockQuantity(String storeId, String productId) {
        StockBean s = em.find(StockBean.class, new StockId(storeId, productId));
        return s == null ? 0 : s.getQuantity();
    }

    /**
     * Adjusts on-hand quantity by a signed delta (positive = delivery, negative =
     * sale). Was a SQLite-specific {@code ON CONFLICT ... DO UPDATE} upsert; now a
     * portable JPA read-modify-write inside the container JTA transaction.
     */
    public void adjustStock(String storeId, String productId, int delta) {
        StockBean s = em.find(StockBean.class, new StockId(storeId, productId));
        if (s == null) {
            s = new StockBean();
            s.setStoreId(storeId);
            s.setProductId(productId);
            s.setQuantity(delta);
        } else {
            s.setQuantity(s.getQuantity() + delta);
        }
        em.merge(s);
    }

    /**
     * Restock guard — true if a DELIVERED purchase order exists for this product
     * at this store. JPQL across PurchaseOrderBean + OrderLineBean (both entities).
     */
    public boolean hasDeliveredOrder(String storeId, String productId) {
        Long count = em.createQuery(
                "SELECT COUNT(po) FROM PurchaseOrderBean po, OrderLineBean ol "
                + "WHERE po.orderId = ol.orderId AND po.storeId = :storeId "
                + "AND ol.productId = :productId AND po.status = 'DELIVERED'", Long.class)
                .setParameter("storeId", storeId)
                .setParameter("productId", productId)
                .getSingleResult();
        return count != null && count > 0;
    }

    // ── stores-table access (now via the StoreBean entity) ─

    /**
     * Lists every store as a {storeId, storeName} map (kept as a Map for the
     * existing callers). JPQL over {@link StoreBean}.
     */
    public List<Map<String, String>> loadStores() {
        List<StoreBean> stores = em.createQuery(
                "SELECT s FROM StoreBean s ORDER BY s.storeId", StoreBean.class).getResultList();
        List<Map<String, String>> out = new ArrayList<>();
        for (StoreBean s : stores) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("storeId", s.getStoreId());
            m.put("storeName", s.getStoreName());
            out.add(m);
        }
        return out;
    }

    /**
     * Inserts a new store. Returns false (without inserting) when the storeId
     * already exists, preserving the old behaviour. Portable JPA.
     */
    public boolean saveStore(String storeId, String storeName) {
        if (em.find(StoreBean.class, storeId) != null) { return false; }
        StoreBean s = new StoreBean();
        s.setStoreId(storeId);
        s.setStoreName(storeName);
        em.persist(s);
        return true;
    }

    public boolean updateStoreName(String storeId, String storeName) {
        StoreBean s = em.find(StoreBean.class, storeId);
        if (s == null) { return false; }
        s.setStoreName(storeName);
        em.merge(s);
        return true;
    }

    public boolean hasAnyStock(String productId) {
        Long count = em.createQuery(
                "SELECT COUNT(s) FROM StockBean s WHERE s.productId = :pid AND s.quantity > 0", Long.class)
                .setParameter("pid", productId).getSingleResult();
        return count != null && count > 0;
    }

    public boolean hasAnyOrders(String productId) {
        Long count = em.createQuery(
                "SELECT COUNT(ol) FROM OrderLineBean ol WHERE ol.productId = :pid", Long.class)
                .setParameter("pid", productId).getSingleResult();
        return count != null && count > 0;
    }

    public void deleteProduct(String productId) {
        ProductBean p = em.find(ProductBean.class, productId);
        if (p != null) { em.remove(p); }
    }

    public boolean hasAnySales(String storeId) {
        Long count = em.createQuery(
                "SELECT COUNT(s) FROM SaleBean s WHERE s.storeId = :sid", Long.class)
                .setParameter("sid", storeId).getSingleResult();
        return count != null && count > 0;
    }

    public boolean hasAnyStockInStore(String storeId) {
        Long count = em.createQuery(
                "SELECT COUNT(s) FROM StockBean s WHERE s.storeId = :sid AND s.quantity > 0", Long.class)
                .setParameter("sid", storeId).getSingleResult();
        return count != null && count > 0;
    }

    public void deleteStore(String storeId) {
        StoreBean s = em.find(StoreBean.class, storeId);
        if (s != null) { em.remove(s); }
    }

    /** Find a product by barcode (exact match). Returns null if not found. */
    public ProductBean findProductByBarcode(String barcode) {
        List<ProductBean> results = em.createQuery(
                "SELECT p FROM ProductBean p WHERE p.barcode = :barcode", ProductBean.class)
                .setParameter("barcode", barcode)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
