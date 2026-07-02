package com.groceryerp.customer;

import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.customer.beans.LoyaltyBean;
import com.groceryerp.customer.beans.PurchaseHistoryBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * CustomerRepository — @Stateless session bean that owns all persistence for the
 * customer domain. Replaces the three nested DAO classes that used to live inside
 * CustomerBean, LoyaltyBean and PurchaseHistoryBean.
 *
 * The container injects a transaction-scoped {@link EntityManager} via
 * {@code @PersistenceContext}; each business method runs in a container-managed
 * JTA transaction (the EJB default is REQUIRED), so there is no manual
 * Connection/commit/close as there was with JDBC.
 */
@Stateless
public class CustomerRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── CustomerBean.DAO replacement ──────────────────────────────

    /** Replaces CustomerBean.DAO.save() — em.merge() emits the dialect-correct upsert. */
    public void saveCustomer(CustomerBean customer) {
        em.merge(customer);
    }

    /** Replaces CustomerBean.DAO.findById() — em.find() replaces the manual SELECT + row-map. */
    public CustomerBean findCustomerById(String customerId) {
        return em.find(CustomerBean.class, customerId);
    }

    /** Backs CustomerServlet's old "SELECT * FROM customers ORDER BY name" join. */
    public List<CustomerBean> findAllCustomers() {
        return em.createQuery(
                "SELECT c FROM CustomerBean c ORDER BY c.name", CustomerBean.class)
                .getResultList();
    }

    // ── LoyaltyBean.DAO replacement ───────────────────────────────

    public LoyaltyBean findLoyaltyByCustomerId(String customerId) {
        return em.find(LoyaltyBean.class, customerId);
    }

    public List<LoyaltyBean> findAllLoyalty() {
        return em.createQuery(
                "SELECT l FROM LoyaltyBean l ORDER BY l.points DESC", LoyaltyBean.class)
                .getResultList();
    }

    /**
     * Replaces LoyaltyBean.DAO.addPoints(). The original used a single SQLite
     * {@code INSERT ... ON CONFLICT ... CASE} statement to atomically add points
     * and recompute the tier. That statement is SQLite-specific and not
     * expressible in portable JPA, so the same logic is implemented here as a
     * read-modify-write within the container-managed transaction.
     *
     * Tier rules preserved exactly: 0-499 BRONZE, 500-1499 SILVER, 1500+ GOLD.
     */
    public void addPoints(String customerId, int points) {
        LoyaltyBean loyalty = em.find(LoyaltyBean.class, customerId);
        if (loyalty == null) {
            loyalty = new LoyaltyBean();
            loyalty.setCustomerId(customerId);
            loyalty.setPoints(0);
        }
        int newTotal = loyalty.getPoints() + points;
        loyalty.setPoints(newTotal);
        loyalty.setTier(tierFor(newTotal));
        em.merge(loyalty);
    }

    private String tierFor(int totalPoints) {
        if (totalPoints >= 1500) { return "GOLD"; }
        if (totalPoints >= 500)  { return "SILVER"; }
        return "BRONZE";
    }

    // ── PurchaseHistoryBean.DAO replacement ───────────────────────

    public void savePurchaseHistory(PurchaseHistoryBean history) {
        em.merge(history);
    }

    /** Replaces PurchaseHistoryBean.DAO.findIdsByCustomer(). */
    public List<String> findSaleIdsByCustomer(String customerId) {
        return em.createQuery(
                "SELECT ph.saleId FROM PurchaseHistoryBean ph WHERE ph.customerId = :cid",
                String.class)
                .setParameter("cid", customerId)
                .getResultList();
    }

    /** Full history rows for a customer (backs CustomerResource history endpoint). */
    public List<PurchaseHistoryBean> findHistoryByCustomer(String customerId) {
        return em.createQuery(
                "SELECT ph FROM PurchaseHistoryBean ph WHERE ph.customerId = :cid ORDER BY ph.date DESC",
                PurchaseHistoryBean.class)
                .setParameter("cid", customerId)
                .setMaxResults(100)
                .getResultList();
    }
}
