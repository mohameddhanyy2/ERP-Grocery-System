package com.groceryerp.reporting.services;

import com.groceryerp.interfaces.*;

/**
 * DataCollector — internal sub-component of the Reporting module.
 *
 * Holds ALL five required interfaces, injected via IoC.
 * This is the clearest IoC demonstration in the project.
 * Never instantiates any module — only calls their interfaces.
 */
public class DataCollector {

    // All five required interfaces — injected via setters (IoC)
    private ISalesData salesData;
    private IStaffData staffData;
    private ITotalStock totalStock;
    private IFinanceData financeData;
    private ICustomerData customerData;

    public DataCollector() {}

    public void setSalesData(ISalesData salesData)       { this.salesData = salesData; }
    public void setStaffData(IStaffData staffData)       { this.staffData = staffData; }
    public void setTotalStock(ITotalStock totalStock)    { this.totalStock = totalStock; }
    public void setFinanceData(IFinanceData financeData) { this.financeData = financeData; }
    public void setCustomerData(ICustomerData customerData) { this.customerData = customerData; }

    public double fetchRevenue(String period)       { return financeData.getTotalRevenue(period); }
    public double fetchExpenses(String period)      { return financeData.getTotalExpenses(period); }
    public double fetchNetProfit(String period)     { return financeData.getNetProfit(period); }
    public double fetchPayroll(String period)       { return staffData.getTotalPayrollCost(period); }
    public int fetchStockLevel(String productId)    { return totalStock.getTotalStock(productId); }
    public int fetchTransactions(String date)       { return salesData.getTransactionCount(date); }
}
