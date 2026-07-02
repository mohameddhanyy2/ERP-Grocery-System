package com.groceryerp.pos;

import com.groceryerp.pos.beans.DiscountBean;
import com.groceryerp.pos.beans.PaymentBean;
import com.groceryerp.pos.beans.ReceiptBean;
import com.groceryerp.pos.beans.SaleBean;
import com.groceryerp.pos.beans.SaleItemBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * POSRepository — @Stateless session bean that owns all persistence for the POS
 * domain. Replaces the five nested DAO classes that used to live inside
 * SaleBean, SaleItemBean, ReceiptBean, PaymentBean and DiscountBean.
 *
 * The container injects a transaction-scoped {@link EntityManager} via
 * {@code @PersistenceContext}; each business method runs in a container-managed
 * JTA transaction (the EJB default is REQUIRED), so there is no manual
 * Connection/commit/close as there was with JDBC.
 */
@Stateless
public class POSRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── SaleBean.DAO replacement ──────────────────────────────────

    /** Replaces SaleBean.DAO.save() — em.merge() emits the dialect-correct upsert
     *  (original SQL: INSERT OR REPLACE INTO sales (...)). */
    public void saveSale(SaleBean sale) {
        em.merge(sale);
    }

    /** Replaces SaleBean.DAO.saveItem() / SaleItemBean.DAO.save()
     *  (original SQL: INSERT OR REPLACE INTO sale_items (...)). */
    public void saveItem(SaleItemBean item) {
        em.merge(item);
    }

    /** Replaces SaleBean.DAO.findById() — em.find() replaces the manual SELECT + row-map. */
    public SaleBean findSaleById(String saleId) {
        return em.find(SaleBean.class, saleId);
    }

    /** Replaces SaleBean.DAO.findByStoreAndDate().
     *  Original SQL: SELECT * FROM sales WHERE storeId = ? AND timestamp LIKE ?
     *  The date-prefix LIKE is reproduced with JPQL LIKE 'date%'. */
    public List<SaleBean> findSalesByStoreAndDate(String storeId, String date) {
        return em.createQuery(
                "SELECT s FROM SaleBean s WHERE s.storeId = :storeId AND s.timestamp LIKE :date",
                SaleBean.class)
                .setParameter("storeId", storeId)
                .setParameter("date", date + "%")
                .getResultList();
    }

    /**
     * Replaces SaleBean.DAO.sumRevenueByStoreAndDate().
     * Original SQLite-specific SQL: SELECT SUM(totalAmount) FROM sales WHERE
     * timestamp LIKE ? [AND storeId = ?]. SQLite's SUM() returns NULL for no
     * rows; that NULL-to-0.0 behavior is reproduced here with COALESCE in JPQL.
     * Pass null/empty storeId to aggregate across all stores.
     */
    public double sumRevenueByStoreAndDate(String storeId, String date) {
        boolean allStores = storeId == null || storeId.isEmpty();
        String jpql = allStores
                ? "SELECT COALESCE(SUM(s.totalAmount), 0.0) FROM SaleBean s WHERE s.timestamp LIKE :date"
                : "SELECT COALESCE(SUM(s.totalAmount), 0.0) FROM SaleBean s WHERE s.storeId = :storeId AND s.timestamp LIKE :date";
        var q = em.createQuery(jpql, Double.class).setParameter("date", date + "%");
        if (!allStores) {
            q.setParameter("storeId", storeId);
        }
        Double result = q.getSingleResult();
        return result != null ? result : 0.0;
    }

    /**
     * Replaces SaleBean.DAO.sumRevenueByCustomer().
     * Original SQLite-specific SQL: SELECT SUM(totalAmount) FROM sales WHERE
     * customerId = ?. SQLite's SUM() returns NULL for no rows; reproduced with
     * COALESCE so the caller still sees 0.0.
     */
    public double sumRevenueByCustomer(String customerId) {
        Double result = em.createQuery(
                "SELECT COALESCE(SUM(s.totalAmount), 0.0) FROM SaleBean s WHERE s.customerId = :cid",
                Double.class)
                .setParameter("cid", customerId)
                .getSingleResult();
        return result != null ? result : 0.0;
    }

    /**
     * Replaces SaleBean.DAO.countByDate().
     * Original SQL: SELECT COUNT(*) FROM sales WHERE timestamp LIKE ?.
     * COUNT(*) never returns NULL, so a plain JPQL count suffices.
     */
    public int countByDate(String date) {
        Long count = em.createQuery(
                "SELECT COUNT(s) FROM SaleBean s WHERE s.timestamp LIKE :date",
                Long.class)
                .setParameter("date", date + "%")
                .getSingleResult();
        return count != null ? count.intValue() : 0;
    }

    /** Replaces SaleBean.DAO.findItemsBySaleId() / SaleItemBean.DAO.findBySaleId().
     *  Original SQL: SELECT * FROM sale_items WHERE saleId = ?. */
    public List<SaleItemBean> findItemsBySaleId(String saleId) {
        return em.createQuery(
                "SELECT i FROM SaleItemBean i WHERE i.saleId = :saleId", SaleItemBean.class)
                .setParameter("saleId", saleId)
                .getResultList();
    }

    /** Replaces SaleItemBean.DAO.findById(). */
    public SaleItemBean findItemById(String itemId) {
        return em.find(SaleItemBean.class, itemId);
    }

    /**
     * Backs PosResource's "all sales" listing. Original servlet SQL:
     *   SELECT s.*, COALESCE(c.name, s.customerId) AS customerName
     *   FROM sales s LEFT JOIN customers c ON s.customerId=c.customerId
     *   ORDER BY s.timestamp DESC LIMIT 200
     * The LEFT JOIN against customers is resolved Java-side in the resource;
     * this method supplies the ordered, limited sales rows.
     */
    public List<SaleBean> findRecentSales() {
        return em.createQuery(
                "SELECT s FROM SaleBean s ORDER BY s.timestamp DESC", SaleBean.class)
                .setMaxResults(200)
                .getResultList();
    }

    /** Store-filtered variant of {@link #findRecentSales()}.
     *  Original servlet SQL added: WHERE s.storeId=? before ORDER BY ... LIMIT 200. */
    public List<SaleBean> findRecentSalesByStore(String storeId) {
        return em.createQuery(
                "SELECT s FROM SaleBean s WHERE s.storeId = :storeId ORDER BY s.timestamp DESC",
                SaleBean.class)
                .setParameter("storeId", storeId)
                .setMaxResults(200)
                .getResultList();
    }

    // ── ReceiptBean.DAO replacement ───────────────────────────────

    /** Replaces ReceiptBean.DAO.save()
     *  (original SQL: INSERT OR REPLACE INTO receipts (...)). */
    public void saveReceipt(ReceiptBean receipt) {
        em.merge(receipt);
    }

    /** Replaces ReceiptBean.DAO.findBySaleId().
     *  Original SQL: SELECT * FROM receipts WHERE saleId = ? (first match). */
    public ReceiptBean findReceiptBySaleId(String saleId) {
        return em.createQuery(
                "SELECT r FROM ReceiptBean r WHERE r.saleId = :saleId", ReceiptBean.class)
                .setParameter("saleId", saleId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    // ── PaymentBean.DAO replacement ───────────────────────────────

    /** Replaces PaymentBean.DAO.save()
     *  (original SQL: INSERT OR REPLACE INTO payments (...)). */
    public void savePayment(PaymentBean payment) {
        em.merge(payment);
    }

    /** Replaces PaymentBean.DAO.findBySaleId().
     *  Original SQL: SELECT * FROM payments WHERE saleId = ? (first match). */
    public PaymentBean findPaymentBySaleId(String saleId) {
        return em.createQuery(
                "SELECT p FROM PaymentBean p WHERE p.saleId = :saleId", PaymentBean.class)
                .setParameter("saleId", saleId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    // ── DiscountBean.DAO replacement ──────────────────────────────

    /** Replaces DiscountBean.DAO.save()
     *  (original SQL: INSERT OR REPLACE INTO discounts (...)). */
    public void saveDiscount(DiscountBean discount) {
        em.merge(discount);
    }

    /** Replaces DiscountBean.DAO.findBySaleId().
     *  Original SQL: SELECT * FROM discounts WHERE saleId = ? (first match). */
    public DiscountBean findDiscountBySaleId(String saleId) {
        return em.createQuery(
                "SELECT d FROM DiscountBean d WHERE d.saleId = :saleId", DiscountBean.class)
                .setParameter("saleId", saleId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
