package com.groceryerp.interfaces;

import jakarta.ejb.Local;
/** Provided by FinanceModule. Required by: ReportingModule. */
@Local
public interface IFinanceData {
    double getTotalRevenue(String period);
    double getTotalExpenses(String period);
    double getNetProfit(String period);
}

// reviewed by: Omar Khalifa