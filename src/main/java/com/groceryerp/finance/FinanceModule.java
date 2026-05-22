package com.groceryerp.finance;

import com.groceryerp.interfaces.*;

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
        // TODO: Member 5 — pull from ISalesData
        return 0.0;
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
        // TODO: Member 5 — build ProfitSummaryBean
        return "Profit summary for " + storeId + " / " + period;
    }
}
