package com.groceryerp.reporting.services;

// @Stateless
// Each fetch method receives a parameter and returns a value immediately.
// No data is accumulated between calls. Holds only injected interface fields.

import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.IFinanceData;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStaffData;
import com.groceryerp.interfaces.ITotalStock;

/**
 * DataCollector — Stateless helper for the Reporting module.
 *
 * Holds all five required interfaces injected via IoC. Each fetch method
 * delegates to one interface and returns immediately — no state accumulated.
 *
 * Bean type: @Stateless — pure delegation, no conversational state.
 */
public class DataCollector {

    // ── All five required interfaces — injected via setters (IoC) ──
    private ISalesData salesData;
    private IStaffData staffData;
    private ITotalStock totalStock;
    private IFinanceData financeData;
    private ICustomerData customerData;

    public DataCollector() { /* no-arg constructor required by IoC */ }

    /** Injects the sales data dependency. */
    public void setSalesData(ISalesData salesData)          { this.salesData = salesData; }

    /** Injects the staff data dependency. */
    public void setStaffData(IStaffData staffData)          { this.staffData = staffData; }

    /** Injects the total stock dependency. */
    public void setTotalStock(ITotalStock totalStock)        { this.totalStock = totalStock; }

    /** Injects the finance data dependency. */
    public void setFinanceData(IFinanceData financeData)    { this.financeData = financeData; }

    /** Injects the customer data dependency. */
    public void setCustomerData(ICustomerData customerData) { this.customerData = customerData; }

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
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock";
        try (java.sql.Connection conn = com.groceryerp.db.DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (java.sql.SQLException e) {
            System.out.println("fetchTotalStockAllProducts failed: " + e.getMessage());
        }
        return 0;
    }

    /** Returns the number of distinct low-stock alert rows for a given store (or all stores if null/blank). */
    public int fetchLowStockCount(String storeId) {
        String sql = (storeId == null || storeId.isBlank())
            ? "SELECT COUNT(*) FROM stock_alerts"
            : "SELECT COUNT(*) FROM stock_alerts WHERE storeId = ?";
        try (java.sql.Connection conn = com.groceryerp.db.DatabaseManager.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            if (storeId != null && !storeId.isBlank()) ps.setString(1, storeId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            System.out.println("fetchLowStockCount failed: " + e.getMessage());
        }
        return 0;
    }
}
