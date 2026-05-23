package com.groceryerp.finance;

// @Stateless
// Chosen because each method computes and returns a result immediately using
// injected interfaces and DAOs. No intermediate values are stored between calls.
// Every invocation is fully self-contained.

import com.groceryerp.finance.beans.ExpenseBean;
import com.groceryerp.finance.beans.RevenueBean;
import com.groceryerp.interfaces.IFinanceData;
import com.groceryerp.interfaces.IOrderStatus;
import com.groceryerp.interfaces.IProfitReport;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStaffData;

/**
 * FinanceModule — Stateless Session Bean for revenue, expense, and profit reporting.
 *
 * Each method delegates to DAOs or injected interfaces and returns immediately.
 * No intermediate values are accumulated between calls.
 *
 * PROVIDED interfaces: IFinanceData, IProfitReport
 * REQUIRED interfaces: ISalesData, IStaffData, IOrderStatus (injected via IoC)
 *
 * Bean type: @Stateless — no conversational state, all data flows through DAOs and interfaces.
 */
public class FinanceModule implements IFinanceData, IProfitReport {

    // ── Injected interfaces (infrastructure, not business state) ──
    private ISalesData salesData;
    private IStaffData staffData;
    private IOrderStatus orderStatus;

    // ── DAO fields (infrastructure, not business state) ───────────
    private final RevenueBean.DAO revenueDao = new RevenueBean.DAO();
    private final ExpenseBean.DAO expenseDao = new ExpenseBean.DAO();

    public FinanceModule() { /* no-arg constructor required by IoC */ }

    /** Injects the sales data dependency. */
    public void setSalesData(ISalesData salesData) { this.salesData = salesData; }

    /** Injects the staff data dependency. */
    public void setStaffData(IStaffData staffData) { this.staffData = staffData; }

    /** Injects the order status dependency. */
    public void setOrderStatus(IOrderStatus orderStatus) { this.orderStatus = orderStatus; }

    // ── IFinanceData (provided) ───────────────────────────────────

    /** Returns the sum of gross revenue from the revenue table for the given period. */
    @Override
    public double getTotalRevenue(String period) {
        return revenueDao.sumByPeriod(period);
    }

    /** Returns total expenses: payroll + purchase orders + misc expenses for the given period. */
    @Override
    public double getTotalExpenses(String period) {
        double payroll  = staffData.getTotalPayrollCost(period);
        double purchase = orderStatus.getTotalPurchaseCost(period);
        double other    = expenseDao.sumByPeriod(period);
        return payroll + purchase + other;
    }

    /** Returns net profit = total revenue − total expenses for the given period. */
    @Override
    public double getNetProfit(String period) {
        return getTotalRevenue(period) - getTotalExpenses(period);
    }

    // ── IProfitReport (provided) ──────────────────────────────────

    /** Builds and returns a profit summary string for the given store and period. */
    @Override
    public String calcProfitSummary(String storeId, String period) {
        double revenue  = getTotalRevenue(period);
        double expenses = getTotalExpenses(period);
        double profit   = revenue - expenses;
        return "Profit Summary [" + storeId + " / " + period + "]"
                + " | Revenue: " + revenue
                + " | Expenses: " + expenses
                + " | Net Profit: " + profit;
    }
}
