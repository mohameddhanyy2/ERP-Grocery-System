package com.groceryerp.interfaces;
/** Provided by FinanceModule. Required by: ReportingModule. */
public interface IFinanceData {
    double getTotalRevenue(String period);
    double getTotalExpenses(String period);
    double getNetProfit(String period);
}
