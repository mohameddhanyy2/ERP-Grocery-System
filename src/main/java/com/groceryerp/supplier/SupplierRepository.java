package com.groceryerp.supplier;

import com.groceryerp.supplier.beans.DeliveryBean;
import com.groceryerp.supplier.beans.OrderLineBean;
import com.groceryerp.supplier.beans.PurchaseOrderBean;
import com.groceryerp.supplier.beans.SupplierBean;
import com.groceryerp.supplier.beans.SupplierProductBean;
import com.groceryerp.supplier.beans.SupplierProductId;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SupplierRepository — @Stateless session bean that owns all persistence for the
 * supplier domain. Replaces the four nested DAO classes that used to live inside
 * SupplierBean, PurchaseOrderBean, OrderLineBean and DeliveryBean, plus the raw
 * JDBC that the old SupplierServlet held directly.
 *
 * The container injects a transaction-scoped {@link EntityManager} via
 * {@code @PersistenceContext}; each business method runs in a container-managed
 * JTA transaction (the EJB default is REQUIRED), so there is no manual
 * Connection/commit/close as there was with JDBC.
 *
 * Two tables in this domain have no JPA @Entity in the project — {@code stores}
 * and {@code supplier_products} (managed only by DatabaseManager DDL) — so the
 * servlet joins that touch them are reproduced here as native queries. The
 * original SQL is preserved verbatim in comments above each one.
 */
@Stateless
public class SupplierRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── SupplierBean.DAO replacement ──────────────────────────────

    /** Replaces SupplierBean.DAO.save() — em.merge() emits the dialect-correct upsert. */
    public void saveSupplier(SupplierBean supplier) {
        em.merge(supplier);
    }

    /** Replaces SupplierBean.DAO.findById() — em.find() replaces the manual SELECT + row-map. */
    public SupplierBean findSupplierById(String supplierId) {
        return em.find(SupplierBean.class, supplierId);
    }

    /** Replaces SupplierBean.DAO.findAllIds() — was "SELECT supplierId FROM suppliers". */
    public List<String> findAllSupplierIds() {
        return em.createQuery(
                "SELECT s.supplierId FROM SupplierBean s", String.class)
                .getResultList();
    }

    /** Backs SupplierResource /suppliers — was "SELECT * FROM suppliers ORDER BY name". */
    public List<SupplierBean> findAllSuppliers() {
        return em.createQuery(
                "SELECT s FROM SupplierBean s ORDER BY s.name", SupplierBean.class)
                .getResultList();
    }

    // ── PurchaseOrderBean.DAO replacement ─────────────────────────

    /** Replaces PurchaseOrderBean.DAO.save() — em.merge() emits the dialect-correct upsert. */
    public void savePurchaseOrder(PurchaseOrderBean order) {
        em.merge(order);
    }

    /** Replaces PurchaseOrderBean.DAO.findAll(). */
    public List<PurchaseOrderBean> findAllPurchaseOrders() {
        return em.createQuery(
                "SELECT po FROM PurchaseOrderBean po", PurchaseOrderBean.class)
                .getResultList();
    }

    /** Replaces PurchaseOrderBean.DAO.findById(). */
    public PurchaseOrderBean findPurchaseOrderById(String orderId) {
        return em.find(PurchaseOrderBean.class, orderId);
    }

    /** Replaces PurchaseOrderBean.DAO.findIdsByStore() — was "... WHERE storeId = ?". */
    public List<String> findOrderIdsByStore(String storeId) {
        return em.createQuery(
                "SELECT po.orderId FROM PurchaseOrderBean po WHERE po.storeId = :storeId", String.class)
                .setParameter("storeId", storeId)
                .getResultList();
    }

    /** Replaces PurchaseOrderBean.DAO.updateStatus() — was "UPDATE purchase_orders SET status=? WHERE orderId=?". */
    public void updateOrderStatus(String orderId, String status) {
        PurchaseOrderBean order = em.find(PurchaseOrderBean.class, orderId);
        if (order != null) {
            order.setStatus(status);
            em.merge(order);
        }
    }

    /**
     * Replaces PurchaseOrderBean.DAO.sumCostByPeriod(). The original used the
     * SQLite-friendly "SELECT SUM(totalCost) ... WHERE orderDate LIKE ?" with a
     * "period%" prefix. Reproduced here with a JPQL LIKE on the same prefix.
     * COALESCE guards the all-null (empty) case that SQL SUM would return as NULL.
     */
    public double sumCostByPeriod(String period) {
        return em.createQuery(
                "SELECT COALESCE(SUM(po.totalCost), 0) FROM PurchaseOrderBean po WHERE po.orderDate LIKE :p", Double.class)
                .setParameter("p", period + "%")
                .getSingleResult();
    }

    // ── OrderLineBean.DAO replacement ─────────────────────────────

    /** Replaces OrderLineBean.DAO.save() — em.merge() emits the dialect-correct upsert. */
    public void saveOrderLine(OrderLineBean line) {
        em.merge(line);
    }

    /** Replaces OrderLineBean.DAO.findByOrderId(). */
    public List<OrderLineBean> findOrderLinesByOrderId(String orderId) {
        return em.createQuery(
                "SELECT ol FROM OrderLineBean ol WHERE ol.orderId = :orderId", OrderLineBean.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    // ── DeliveryBean.DAO replacement ──────────────────────────────

    /** Replaces DeliveryBean.DAO.save() — em.merge() emits the dialect-correct upsert. */
    public void saveDelivery(DeliveryBean delivery) {
        em.merge(delivery);
    }

    /** Replaces DeliveryBean.DAO.findByOrderId(). */
    public DeliveryBean findDeliveryByOrderId(String orderId) {
        List<DeliveryBean> rows = em.createQuery(
                "SELECT d FROM DeliveryBean d WHERE d.orderId = :orderId", DeliveryBean.class)
                .setParameter("orderId", orderId)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Replaces DeliveryBean.DAO.updateStatus() — was "UPDATE deliveries SET deliveryStatus=? WHERE deliveryId=?". */
    public void updateDeliveryStatus(String deliveryId, String status) {
        DeliveryBean delivery = em.find(DeliveryBean.class, deliveryId);
        if (delivery != null) {
            delivery.setDeliveryStatus(status);
            em.merge(delivery);
        }
    }

    // ── Cross-domain read used by placeOrder ──────────────────────

    /**
     * Looks up a product to price an order line. Was: {@code new ProductBean.DAO()
     * .findById(productId)} inside SupplierModule.placeOrder(). ProductBean is a
     * JPA entity owned by the inventory domain, so a plain em.find() suffices.
     */
    public com.groceryerp.inventory.beans.ProductBean findProductById(String productId) {
        return em.find(com.groceryerp.inventory.beans.ProductBean.class, productId);
    }

    /**
     * Updates the store-side selling price. No-op if the product is unknown.
     */
    public void updateProductPrice(String productId, double price) {
        com.groceryerp.inventory.beans.ProductBean p =
                em.find(com.groceryerp.inventory.beans.ProductBean.class, productId);
        if (p != null) {
            p.setPrice(price);
            em.merge(p);
        }
    }

    /**
     * Updates the unit cost (purchase price) on both the product and the
     * supplier_products row. Called when a supplier submits a quote so the
     * unit cost stays in sync without touching the selling price.
     */
    public void updateProductUnitCost(String supplierId, String productId, double unitCost) {
        com.groceryerp.inventory.beans.ProductBean p =
                em.find(com.groceryerp.inventory.beans.ProductBean.class, productId);
        if (p != null) {
            p.setUnitCost(unitCost);
            em.merge(p);
        }
        SupplierProductBean sp = em.find(SupplierProductBean.class,
                new SupplierProductId(supplierId, productId));
        if (sp != null) {
            sp.setUnitCost(unitCost);
            em.merge(sp);
        }
    }

    // ── SupplierServlet raw-SQL replacement (web layer SQL moved here) ──

    /**
     * Backs SupplierResource /orders. Original servlet SQL:
     *   SELECT po.*, s.name as supplierName, st.storeName FROM purchase_orders po
     *   LEFT JOIN suppliers s ON po.supplierId=s.supplierId
     *   LEFT JOIN stores st ON po.storeId=st.storeId
     *   [WHERE po.storeId=? | WHERE po.supplierId=?] ORDER BY po.orderDate DESC [LIMIT 200]
     *
     * 'stores' has no JPA entity, so this stays a native query. supplierId/storeId
     * are mutually exclusive filters (null when not filtering), matching the servlet.
     */
    public List<Map<String, Object>> findOrdersWithNames(String storeId, String supplierId) {
        String base = "SELECT po.orderId, po.supplierId, s.name AS supplierName, po.storeId, " +
                      "st.storeName, po.orderDate, po.totalCost, po.status " +
                      "FROM purchase_orders po " +
                      "LEFT JOIN suppliers s ON po.supplierId = s.supplierId " +
                      "LEFT JOIN stores st ON po.storeId = st.storeId ";
        String sql;
        if (storeId != null)          { sql = base + "WHERE po.storeId = :storeId ORDER BY po.orderDate DESC"; }
        else if (supplierId != null)  { sql = base + "WHERE po.supplierId = :supplierId ORDER BY po.orderDate DESC"; }
        else                          { sql = base + "ORDER BY po.orderDate DESC LIMIT 200"; }

        var q = em.createNativeQuery(sql);
        if (storeId != null)          { q.setParameter("storeId", storeId); }
        else if (supplierId != null)  { q.setParameter("supplierId", supplierId); }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : q.getResultList()) {
            Object[] r = (Object[]) o;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", r[0]);
            row.put("supplierId", r[1]);
            row.put("supplierName", r[2]);
            row.put("storeId", r[3]);
            row.put("storeName", r[4]);
            row.put("orderDate", r[5]);
            row.put("totalCost", r[6]);
            row.put("status", r[7]);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Backs SupplierResource /orderlines. Original servlet SQL:
     *   SELECT ol.*, p.name as productName FROM order_lines ol
     *   LEFT JOIN products p ON ol.productId=p.productId WHERE ol.orderId=?
     *
     * Reproduced with JPQL across the OrderLineBean and ProductBean entities.
     */
    public List<Map<String, Object>> findOrderLinesWithProductName(String orderId) {
        List<Object[]> results = em.createQuery(
                "SELECT ol, p.name FROM OrderLineBean ol " +
                "LEFT JOIN ProductBean p ON p.productId = ol.productId " +
                "WHERE ol.orderId = :orderId", Object[].class)
                .setParameter("orderId", orderId)
                .getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : results) {
            OrderLineBean ol = (OrderLineBean) r[0];
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineId", ol.getLineId());
            row.put("orderId", ol.getOrderId());
            row.put("productId", ol.getProductId());
            row.put("productName", r[1] != null ? r[1] : ol.getProductId());
            row.put("quantity", ol.getQuantity());
            row.put("unitPrice", ol.getUnitPrice());
            rows.add(row);
        }
        return rows;
    }

    /**
     * Backs SupplierResource /stockalerts. Original servlet SQL (two variants on
     * whether supplierId is supplied) joined stock_alerts to products, stores,
     * supplier_products and purchase_orders. 'stores' and 'supplier_products' have
     * no JPA entity, so this is reproduced as a native query.
     *
     * supplierId variant SQL:
     *   SELECT sa.alertId, sa.productId, COALESCE(p.name, sa.productId) AS productName,
     *          sa.storeId, COALESCE(st.storeName, sa.storeId) AS storeName,
     *          sa.currentQty, sa.threshold, sa.alertDate,
     *          po.orderId, po.status AS orderStatus, po.supplierId AS assignedSupplierId
     *   FROM stock_alerts sa
     *   INNER JOIN supplier_products sp ON sp.productId = sa.productId AND sp.supplierId = ?
     *   LEFT JOIN products p ON sa.productId = p.productId
     *   LEFT JOIN stores st ON sa.storeId = st.storeId
     *   LEFT JOIN purchase_orders po ON po.productAlertId = sa.alertId
     *   WHERE po.orderId IS NULL OR po.supplierId = ?
     *   ORDER BY sa.alertDate DESC
     * (no-supplierId variant: same without the supplier_products INNER JOIN / WHERE)
     */
    public List<Map<String, Object>> findStockAlerts(String supplierId) {
        String sql;
        if (supplierId != null) {
            sql = "SELECT sa.alertId, sa.productId, COALESCE(p.name, sa.productId) AS productName, " +
                  "sa.storeId, COALESCE(st.storeName, sa.storeId) AS storeName, " +
                  "sa.currentQty, sa.threshold, sa.alertDate, " +
                  "po.orderId, po.status AS orderStatus, po.supplierId AS assignedSupplierId " +
                  "FROM stock_alerts sa " +
                  "INNER JOIN supplier_products sp ON sp.productId = sa.productId AND sp.supplierId = :sid " +
                  "LEFT JOIN products p ON sa.productId = p.productId " +
                  "LEFT JOIN stores st ON sa.storeId = st.storeId " +
                  "LEFT JOIN purchase_orders po ON po.productAlertId = sa.alertId " +
                  "WHERE po.orderId IS NULL " +
                  "ORDER BY sa.alertDate DESC";
        } else {
            sql = "SELECT sa.alertId, sa.productId, COALESCE(p.name, sa.productId) AS productName, " +
                  "sa.storeId, COALESCE(st.storeName, sa.storeId) AS storeName, " +
                  "sa.currentQty, sa.threshold, sa.alertDate, " +
                  "po.orderId, po.status AS orderStatus, po.supplierId AS assignedSupplierId " +
                  "FROM stock_alerts sa " +
                  "LEFT JOIN products p ON sa.productId = p.productId " +
                  "LEFT JOIN stores st ON sa.storeId = st.storeId " +
                  "LEFT JOIN purchase_orders po ON po.productAlertId = sa.alertId " +
                  "WHERE po.orderId IS NULL " +
                  "ORDER BY sa.alertDate DESC";
        }
        var q = em.createNativeQuery(sql);
        if (supplierId != null) { q.setParameter("sid", supplierId); }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object o : q.getResultList()) {
            Object[] r = (Object[]) o;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("alertId", r[0]);
            row.put("productId", r[1]);
            row.put("productName", r[2]);
            row.put("storeId", r[3]);
            row.put("storeName", r[4]);
            row.put("currentQty", r[5]);
            row.put("threshold", r[6]);
            row.put("alertDate", r[7]);
            row.put("orderId", r[8]);
            row.put("orderStatus", r[9]);
            row.put("assignedSupplierId", r[10]);
            rows.add(row);
        }
        return rows;
    }

    /**
     * Backs SupplierResource /products (GET). Original servlet SQL:
     *   SELECT sp.productId, COALESCE(p.name, sp.productId) AS productName
     *   FROM supplier_products sp LEFT JOIN products p ON sp.productId = p.productId
     *   WHERE sp.supplierId = ? ORDER BY productName
     *
     * 'supplier_products' has no JPA entity, so this stays a native query.
     */
    public List<Map<String, Object>> findSupplierProducts(String supplierId) {
        List<Object[]> rows = em.createQuery(
                "SELECT sp.productId, p.name, sp.unitCost FROM SupplierProductBean sp " +
                "LEFT JOIN ProductBean p ON p.productId = sp.productId " +
                "WHERE sp.supplierId = :sid ORDER BY p.name", Object[].class)
                .setParameter("sid", supplierId)
                .getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", r[0]);
            row.put("productName", r[1] != null ? r[1] : r[0]);
            row.put("unitCost", r[2] != null ? ((Number) r[2]).doubleValue() : 0.0);
            out.add(row);
        }
        return out;
    }

    /**
     * Backs SupplierResource POST /products. Original servlet SQL:
     *   INSERT OR IGNORE INTO supplier_products (supplierId, productId) VALUES (?, ?)
     *
     * INSERT OR IGNORE is SQLite-specific. Reimplemented in Java as a "check then
     * insert" so the operation stays idempotent on any dialect: only insert when
     * the (supplierId, productId) pair does not already exist.
     */
    public void assignSupplierProduct(String supplierId, String productId) {
        assignSupplierProduct(supplierId, productId, 0.0);
    }

    public void assignSupplierProduct(String supplierId, String productId, double unitCost) {
        SupplierProductBean existing = em.find(SupplierProductBean.class, new SupplierProductId(supplierId, productId));
        if (existing == null) {
            em.persist(new SupplierProductBean(supplierId, productId, unitCost));
        } else if (unitCost > 0) {
            existing.setUnitCost(unitCost);
            em.merge(existing);
        }
    }

    /**
     * Returns a map of productId -> comma-separated supplier names, for every
     * product that has at least one supplier. Used by the frontend to show the
     * supplier next to each product on the Inventory / POS / alerts views. A
     * product may have several suppliers (supplier_products is many-to-many), so
     * names are joined into one display string, sorted for stable output.
     *
     * 'supplier_products' / 'suppliers' joined as native query (no JPA entity for
     * supplier_products):
     *   SELECT sp.productId, s.name FROM supplier_products sp
     *   JOIN suppliers s ON s.supplierId = sp.supplierId
     */
    public Map<String, String> productSupplierNames() {
        List<Object[]> rows = em.createQuery(
                "SELECT sp.productId, s.name FROM SupplierProductBean sp, SupplierBean s " +
                "WHERE s.supplierId = sp.supplierId " +
                "ORDER BY sp.productId, s.name", Object[].class).getResultList();
        Map<String, String> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String productId = (String) r[0];
            String name = (String) r[1];
            map.merge(productId, name, (existing, add) -> existing + ", " + add);
        }
        return map;
    }

    /**
     * Returns the supplierIds whose catalogue includes the given product. Used by
     * {@link com.groceryerp.api.AlertBroadcaster} to decide which connected supplier
     * SSE clients should receive a low-stock alert. This is the inverse of
     * {@link #findSupplierProducts(String)}: there we list a supplier's products,
     * here we list a product's suppliers.
     *
     * 'supplier_products' has no JPA entity, so this stays a native query:
     *   SELECT supplierId FROM supplier_products WHERE productId = ?
     */
    /**
     * Returns the most recent unit price this supplier quoted for this product
     * (the latest order_lines.unitPrice across that supplier's purchase orders),
     * or null if they have never quoted it. Used to pre-fill the quote form so the
     * supplier defaults to their last price.
     *
     * order_lines has a JPA entity but the supplier lives on purchase_orders, so
     * this joins the two; ordering by orderId DESC approximates "most recent"
     * because orderIds are timestamp-based ("ORD-<millis>").
     */
    public Double lastUnitPrice(String supplierId, String productId) {
        List<Double> prices = em.createQuery(
                "SELECT ol.unitPrice FROM OrderLineBean ol, PurchaseOrderBean po " +
                "WHERE ol.orderId = po.orderId AND po.supplierId = :sid AND ol.productId = :pid " +
                "ORDER BY po.orderId DESC", Double.class)
                .setParameter("sid", supplierId)
                .setParameter("pid", productId)
                .setMaxResults(1)
                .getResultList();
        return prices.isEmpty() ? null : prices.get(0);
    }

    public List<String> findSupplierIdsForProduct(String productId) {
        return em.createQuery(
                "SELECT sp.supplierId FROM SupplierProductBean sp WHERE sp.productId = :pid",
                String.class)
                .setParameter("pid", productId)
                .getResultList();
    }

    /**
     * Backs SupplierResource POST /removeproduct. Original servlet SQL:
     *   DELETE FROM supplier_products WHERE supplierId = ? AND productId = ?
     */
    public void removeSupplierProduct(String supplierId, String productId) {
        SupplierProductBean sp = em.find(SupplierProductBean.class,
                new SupplierProductId(supplierId, productId));
        if (sp != null) { em.remove(sp); }
    }

    public void removeAllSupplierProductLinks(String productId) {
        em.createQuery("DELETE FROM SupplierProductBean sp WHERE sp.productId = :pid")
                .setParameter("pid", productId).executeUpdate();
    }

    public void updateSupplier(String supplierId, String name, String contactEmail, int leadTimeDays) {
        SupplierBean s = em.find(SupplierBean.class, supplierId);
        if (s != null) {
            if (name != null)         { s.setName(name); }
            if (contactEmail != null) { s.setContactEmail(contactEmail); }
            if (leadTimeDays > 0)     { s.setLeadTimeDays(leadTimeDays); }
            em.merge(s);
        }
    }

    public boolean hasAnyOrders(String supplierId) {
        Long count = em.createQuery(
                "SELECT COUNT(po) FROM PurchaseOrderBean po WHERE po.supplierId = :sid", Long.class)
                .setParameter("sid", supplierId).getSingleResult();
        return count != null && count > 0;
    }

    public boolean hasAnyProducts(String supplierId) {
        Long count = em.createQuery(
                "SELECT COUNT(sp) FROM SupplierProductBean sp WHERE sp.supplierId = :sid", Long.class)
                .setParameter("sid", supplierId).getSingleResult();
        return count != null && count > 0;
    }

    public void deleteSupplier(String supplierId) {
        SupplierBean s = em.find(SupplierBean.class, supplierId);
        if (s != null) { em.remove(s); }
    }

    /**
     * Looks up productId + storeId for a stock alert (quote flow). Original SQL:
     *   SELECT productId, storeId FROM stock_alerts WHERE alertId = ?
     *
     * Uses the StockAlertBean entity (mapped to stock_alerts). Returns a 2-element
     * String[]{productId, storeId}, or null if the alert is unknown.
     */
    public String[] findAlertProductAndStore(String alertId) {
        com.groceryerp.inventory.beans.StockAlertBean alert =
                em.find(com.groceryerp.inventory.beans.StockAlertBean.class, alertId);
        if (alert == null) { return null; }
        return new String[]{ alert.getProductId(), alert.getStoreId() };
    }

    /**
     * Backs SupplierResource POST /accept and /outfordelivery — guarded status
     * transitions that only fire when the order is in the expected current state.
     * Original SQL e.g.:
     *   UPDATE purchase_orders SET status='ACCEPTED' WHERE orderId=? AND status='QUOTED'
     *
     * Returns the number of rows changed (0 ⇒ order missing or in the wrong state),
     * preserving the servlet's 409 behaviour.
     */
    public int transitionOrderStatus(String orderId, String fromStatus, String toStatus) {
        return em.createQuery(
                "UPDATE PurchaseOrderBean po SET po.status = :to WHERE po.orderId = :id AND po.status = :from")
                .setParameter("to", toStatus)
                .setParameter("from", fromStatus)
                .setParameter("id", orderId)
                .executeUpdate();
    }
}
