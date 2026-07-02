package com.groceryerp.reporting.services;

import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.IFinanceData;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStaffData;
import com.groceryerp.interfaces.ITotalStock;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * DataCollector — @Stateless helper for the Reporting module.
 *
 * Holds all five required interfaces. These were previously wired in via
 * IoC setters (called from ReportingModule, which in turn was wired in Main).
 * They are now injected directly by the container via {@code @Inject}, so the
 * setSalesData/setStaffData/setTotalStock/setFinanceData/setCustomerData setters
 * are gone.
 *
 * Each fetch method delegates to one interface and returns immediately — no
 * state accumulated between calls. Bean type: @Stateless — pure delegation.
 *
 * NOTE: fetchTotalStockAllProducts() and fetchLowStockCount() are cross-table
 * aggregates over the {@code stock} / {@code stock_alerts} tables, which have no
 * owning @Entity in this module. They were raw JDBC via DatabaseManager; now they
 * run through the container EntityManager as native queries (same SQL), so the
 * report output is identical but there is no more manual Connection/JDBC and no
 * dependency on the deleted DatabaseManager.
 */
@Stateless
public class DataCollector {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── All five required interfaces — container-injected via CDI ──
    @Inject
    private ISalesData salesData;
    @Inject
    private IStaffData staffData;
    @Inject
    private ITotalStock totalStock;
    @Inject
    private IFinanceData financeData;
    @Inject
    private ICustomerData customerData;

    public DataCollector() { /* required no-arg constructor for the container */ }

    /** Fetches total revenue for the given period from IFinanceData. */
    public double fetchRevenue(String period)    { return financeData.getTotalRevenue(period); }

    /** Fetches total expenses for the given period from IFinanceData. */
    public double fetchExpenses(String period)   { return financeData.getTotalExpenses(period); }

    /** Fetches net profit for the given period from IFinanceData. */
    public double fetchNetProfit(String period)  { return financeData.getNetProfit(period); }

    /** Fetches total payroll cost for the given period from IStaffData. */
    public double fetchPayroll(String period)    { return staffData.getTotalPayrollCost(period); }

    /** Fetches total stock level for the given product ID from ITotalStock. */
    public int fetchStockLevel(String productId) { return totalStock.getTotalStock(productId); }

    /** Fetches transaction count for the given date from ISalesData. */
    public int fetchTransactions(String date)    { return salesData.getTransactionCount(date); }

    /** Returns the sum of all stock quantities across every product and store. */
    public int fetchTotalStockAllProducts() {
        // Native query — 'stock' has no @Entity. Was DatabaseManager raw JDBC.
        Number total = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(quantity), 0) FROM stock").getSingleResult();
        return total == null ? 0 : total.intValue();
    }

    /** Returns the number of distinct low-stock alert rows for a given store (or all stores if null/blank). */
    public int fetchLowStockCount(String storeId) {
        // Native query — 'stock_alerts' aggregate, no @Entity COUNT here. Was DatabaseManager raw JDBC.
        Number count;
        if (storeId == null || storeId.isBlank()) {
            count = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM stock_alerts").getSingleResult();
        } else {
            count = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM stock_alerts WHERE storeId = :storeId")
                    .setParameter("storeId", storeId)
                    .getSingleResult();
        }
        return count == null ? 0 : count.intValue();
    }
}
