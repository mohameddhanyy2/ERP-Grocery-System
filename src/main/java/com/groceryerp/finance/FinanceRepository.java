package com.groceryerp.finance;

import com.groceryerp.finance.beans.ExpenseBean;
import com.groceryerp.finance.beans.RevenueBean;
import com.groceryerp.finance.beans.TaxBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * FinanceRepository — @Stateless session bean that owns all persistence for the
 * finance domain. Replaces the nested DAO classes that used to live inside
 * ExpenseBean, RevenueBean and TaxBean.
 *
 * The container injects a transaction-scoped {@link EntityManager} via
 * {@code @PersistenceContext}; each business method runs in a container-managed
 * JTA transaction (the EJB default is REQUIRED), so there is no manual
 * Connection/commit/close as there was with JDBC.
 *
 * SUM aggregates (sumByPeriod / sumCostByPeriod) are reproduced with JPQL
 * {@code SELECT SUM(...)}. JPQL SUM over a double maps to a {@code Double} that
 * is {@code null} when no rows match — the old JDBC {@code rs.getDouble(1)}
 * returned 0.0 in that case, so each method coalesces null to 0.0 to preserve
 * the original contract.
 */
@Stateless
public class FinanceRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── ExpenseBean.DAO replacement ───────────────────────────────

    /**
     * Replaces ExpenseBean.DAO.save().
     * Old SQL: INSERT OR REPLACE INTO expenses (expenseId,storeId,category,amount,date) VALUES (?,?,?,?,?)
     * em.merge() emits the dialect-correct upsert.
     */
    public void saveExpense(ExpenseBean expense) {
        em.merge(expense);
    }

    /**
     * Replaces ExpenseBean.DAO.sumByPeriod().
     * Old SQL: SELECT SUM(amount) FROM expenses WHERE date LIKE ?   (param = period + "%")
     * The SQLite LIKE prefix-match is reproduced with a JPQL LIKE and a concatenated
     * wildcard; SUM(null rows) -> null is coalesced to 0.0 to match rs.getDouble.
     */
    public double sumExpensesByPeriod(String period) {
        Double total = em.createQuery(
                "SELECT SUM(e.amount) FROM ExpenseBean e WHERE e.date LIKE :period",
                Double.class)
                .setParameter("period", period + "%")
                .getSingleResult();
        return total != null ? total : 0.0;
    }

    /** Backs FinanceServlet's "SELECT * FROM expenses ORDER BY date DESC LIMIT 200". */
    public List<ExpenseBean> findAllExpenses() {
        return em.createQuery(
                "SELECT e FROM ExpenseBean e ORDER BY e.date DESC", ExpenseBean.class)
                .setMaxResults(200)
                .getResultList();
    }

    // ── RevenueBean.DAO replacement ───────────────────────────────

    /**
     * Replaces RevenueBean.DAO.save().
     * Old SQL: INSERT OR REPLACE INTO revenue (revenueId,period,storeId,grossRevenue) VALUES (?,?,?,?)
     */
    public void saveRevenue(RevenueBean revenue) {
        em.merge(revenue);
    }

    /**
     * Replaces RevenueBean.DAO.sumByPeriod().
     * Old SQL: SELECT SUM(grossRevenue) FROM revenue WHERE period = ?
     * SUM(null rows) -> null coalesced to 0.0 to match rs.getDouble.
     */
    public double sumRevenueByPeriod(String period) {
        Double total = em.createQuery(
                "SELECT SUM(r.grossRevenue) FROM RevenueBean r WHERE r.period = :period",
                Double.class)
                .setParameter("period", period)
                .getSingleResult();
        return total != null ? total : 0.0;
    }

    /** Backs FinanceServlet's "SELECT * FROM revenue ORDER BY period DESC LIMIT 200". */
    public List<RevenueBean> findAllRevenue() {
        return em.createQuery(
                "SELECT r FROM RevenueBean r ORDER BY r.period DESC", RevenueBean.class)
                .setMaxResults(200)
                .getResultList();
    }

    // ── TaxBean.DAO replacement ───────────────────────────────────

    /**
     * Replaces TaxBean.DAO.save().
     * Old SQL: INSERT OR REPLACE INTO tax_records (taxId,period,taxRate,taxableAmount,taxOwed) VALUES (?,?,?,?,?)
     */
    public void saveTax(TaxBean tax) {
        em.merge(tax);
    }

    /**
     * Replaces TaxBean.DAO.findByPeriod().
     * Old SQL: SELECT taxId,period,taxRate,taxableAmount,taxOwed FROM tax_records WHERE period = ?
     * Returns null when no row matches (getResultStream/findFirst preserves the
     * original "return null if not found" contract without throwing).
     */
    public TaxBean findTaxByPeriod(String period) {
        return em.createQuery(
                "SELECT t FROM TaxBean t WHERE t.period = :period", TaxBean.class)
                .setParameter("period", period)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /** Backs FinanceServlet's "SELECT * FROM tax_records ORDER BY period DESC LIMIT 100". */
    public List<TaxBean> findAllTax() {
        return em.createQuery(
                "SELECT t FROM TaxBean t ORDER BY t.period DESC", TaxBean.class)
                .setMaxResults(100)
                .getResultList();
    }
}
