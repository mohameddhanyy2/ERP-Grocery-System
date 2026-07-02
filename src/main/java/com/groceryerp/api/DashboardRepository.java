package com.groceryerp.api;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardRepository — @Stateless bean holding the cross-table aggregate queries
 * that used to be raw JDBC inside DashboardServlet. These intentionally use
 * native queries (createNativeQuery) because the dashboard is a read-only
 * reporting view that spans tables owned by several modules (sales, stores,
 * stock, products) — the same joins the servlet did, now centralized and
 * transaction-managed.
 *
 * NOTE: native SQL keeps these queries coupled to the SQLite dialect (e.g.
 * substr(), COALESCE). For true portability they would need per-dialect variants
 * or JPQL equivalents; documented in MIGRATION_EJB_JPA.md as a residual coupling.
 */
@Stateless
public class DashboardRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    /** COUNT(*) for a known table (replaces DashboardServlet.countTable). */
    public int countTable(String table) {
        // table is from a fixed internal allow-list in DashboardResource, never user input.
        Object result = em.createNativeQuery("SELECT COUNT(*) FROM " + table).getSingleResult();
        return ((Number) result).intValue();
    }

    /** Sales aggregated by day, last 30 days. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> salesByDay() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT substr(timestamp,1,10) as day, COUNT(*) as txCount, SUM(totalAmount) as total " +
                "FROM sales GROUP BY substr(timestamp,1,10) ORDER BY day DESC LIMIT 30").getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", r[0]);
            m.put("transactions", ((Number) r[1]).intValue());
            m.put("revenue", r[2] == null ? 0.0 : ((Number) r[2]).doubleValue());
            out.add(m);
        }
        return out;
    }

    /** Revenue + transaction count by store. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> revenueByStore() {
        // Postgres requires every non-aggregated selected column in GROUP BY
        // (unlike SQLite, which tolerated grouping by storeId alone). Group by both
        // s.storeId and st.storeName so COALESCE(st.storeName, ...) is valid.
        List<Object[]> rows = em.createNativeQuery(
                "SELECT s.storeId, COALESCE(st.storeName, s.storeId) as storeName, COUNT(*) as txCount, " +
                "SUM(s.totalAmount) as total FROM sales s LEFT JOIN stores st ON s.storeId=st.storeId " +
                "GROUP BY s.storeId, st.storeName")
                .getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storeId", r[1]);
            m.put("transactions", ((Number) r[2]).intValue());
            m.put("revenue", r[3] == null ? 0.0 : ((Number) r[3]).doubleValue());
            out.add(m);
        }
        return out;
    }

    /** Stock quantity by product category. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> stockByCategory() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT p.category, SUM(s.quantity) as total FROM stock s " +
                "JOIN products p ON s.productId=p.productId GROUP BY p.category").getResultList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category", r[0]);
            m.put("quantity", r[1] == null ? 0 : ((Number) r[1]).intValue());
            out.add(m);
        }
        return out;
    }

    /** storeId → storeName map (replaces DashboardServlet.resolveStoreNames). */
    @SuppressWarnings("unchecked")
    public Map<String, String> storeNames() {
        List<Object[]> rows = em.createNativeQuery("SELECT storeId, storeName FROM stores").getResultList();
        Map<String, String> map = new LinkedHashMap<>();
        for (Object[] r : rows) { map.put((String) r[0], (String) r[1]); }
        return map;
    }
}
