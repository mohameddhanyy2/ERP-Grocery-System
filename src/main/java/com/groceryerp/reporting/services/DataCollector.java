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
}
