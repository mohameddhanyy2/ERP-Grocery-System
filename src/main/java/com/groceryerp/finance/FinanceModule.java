package com.groceryerp.finance;

import java.time.LocalDate;

import com.groceryerp.finance.beans.ExpenseBean;
import com.groceryerp.interfaces.IFinanceData;
import com.groceryerp.interfaces.IOrderStatus;
import com.groceryerp.interfaces.IProfitReport;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStaffData;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * FinanceModule — now a real @Stateless session bean (previously a plain object
 * manually instantiated and wired in Main.java).
 *
 * Each method computes and returns a result immediately using injected
 * interfaces and the persistence repository. No conversational state is held
 * between calls.
 *
 * PROVIDED interfaces: IFinanceData, IProfitReport
 * REQUIRED interfaces: ISalesData, IStaffData, IOrderStatus — injected by the
 *                      container via @Inject instead of the old setSalesData /
 *                      setStaffData / setOrderStatus setter calls in Main.
 *
 * Persistence is delegated to the injected @Stateless {@link FinanceRepository}
 * (the old RevenueBean.DAO / ExpenseBean.DAO fields).
 */
@Stateless
@LocalBean
public class FinanceModule implements IFinanceData, IProfitReport {

    /** Container-injected persistence service (replaces the nested DAO fields). */
    @EJB
    private FinanceRepository repository;

    // ── Injected interfaces (infrastructure, not business state) ──
    @Inject
    private ISalesData salesData;

    @Inject
    private IStaffData staffData;

    @Inject
    private IOrderStatus orderStatus;

    public FinanceModule() { /* required no-arg constructor for the container */ }

    public void recordPurchaseCost(String storeId, String productId, int quantity, double totalCost) {
        ExpenseBean expense = new ExpenseBean();
        expense.setAmount(totalCost);
        expense.setStoreId(storeId);
        expense.setCategory("PURCHASE");
        expense.setExpenseId("EXP-" + System.currentTimeMillis());
        expense.setDate(LocalDate.now().toString());
        repository.saveExpense(expense);
    }

    // ── IFinanceData (provided) ───────────────────────────────────

    /** Returns the sum of gross revenue from the revenue table for the given period. */
    @Override
    public double getTotalRevenue(String period) {
        return repository.sumRevenueByPeriod(period);
    }

    /** Returns total expenses: payroll + all expenses (including PURCHASE rows) for the given period. */
    @Override
    public double getTotalExpenses(String period) {
        double payroll = staffData.getTotalPayrollCost(period);
        double other   = repository.sumExpensesByPeriod(period);
        return payroll + other;
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

    /**
     * Calculate overall profit (simple aggregate) using injected services.
     */
    @Override
    public double calcProfit() {
        return getNetProfit(java.time.YearMonth.now().toString());
    }

}

// conflicts resolved by: Omar Khalifa
