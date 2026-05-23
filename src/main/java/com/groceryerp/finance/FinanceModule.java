package com.groceryerp.finance;

import com.groceryerp.interfaces.*;
import com.groceryerp.finance.beans.ProfitSummaryBean;

import java.util.Objects;

/**
 * Finance Module — Component implementation.
 *
 * PROVIDED interfaces: IFinanceData, IProfitReport
 * REQUIRED interfaces: ISalesData, IStaffData, IOrderStatus (injected via IoC)
 *
 * This module is the strongest IoC demonstration in the project:
 * it depends on THREE required interfaces, all injected externally.
 */
public class FinanceModule implements IFinanceData, IProfitReport {

    // Required interfaces — injected via IoC (never instantiated here)
    private ISalesData salesData;
    private IStaffData staffData;
    private IOrderStatus orderStatus;

    public FinanceModule() {}

    public void setSalesData(ISalesData salesData) { this.salesData = salesData; }
    public void setStaffData(IStaffData staffData) { this.staffData = staffData; }
    public void setOrderStatus(IOrderStatus orderStatus) { this.orderStatus = orderStatus; }

    @Override
    public double getTotalRevenue(String period) {
        if (salesData == null) return 0.0;
        try {
            // delegate to salesData; many implementations expose store-level revenue, accept a special token
            return salesData.getTotalRevenueBySale("ALL", period);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    @Override
    public double getTotalExpenses(String period) {
        double payroll  = staffData.getTotalPayrollCost(period);
        double purchase = orderStatus.getTotalPurchaseCost(period);
        return payroll + purchase;
    }

    @Override
    public double getNetProfit(String period) {
        return getTotalRevenue(period) - getTotalExpenses(period);
    }

    @Override
    public String calcProfitSummary(String storeId, String period) {
        ProfitSummaryBean summary = getFinancialSummary();
        if (Objects.nonNull(summary)) {
            return summary.toString();
        }
        return "No summary available for " + storeId + " / " + period;
    }

    /**
     * Calculate overall profit (simple aggregate) using injected services.
     */
    public double calcProfit() {
        if (salesData == null || staffData == null || orderStatus == null) return 0.0;
        double revenue = getTotalRevenue("ALL");
        double payroll = staffData.getTotalPayrollCost("ALL");
        double purchaseCosts = orderStatus.getTotalPurchaseCost("ALL");
        return revenue - payroll - purchaseCosts;
    }

    /**
     * Build a ProfitSummaryBean for a generic period (delegates to injected services).
     */
    public ProfitSummaryBean getFinancialSummary() {
        if (salesData == null || staffData == null || orderStatus == null) return null;
        double revenue = getTotalRevenue("ALL");
        double payroll = staffData.getTotalPayrollCost("ALL");
        double purchaseCosts = orderStatus.getTotalPurchaseCost("ALL");
        double totalExpenses = payroll + purchaseCosts;
        double net = revenue - totalExpenses;
        return new ProfitSummaryBean("ALL", revenue, totalExpenses, net);
    }
}
