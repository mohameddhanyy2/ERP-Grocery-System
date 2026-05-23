package com.groceryerp.interfaces;
/** Provided by FinanceModule. Returns profit summary per store per period. */
public interface IProfitReport {
    String calcProfitSummary(String storeId, String period);
    double calcProfit();
}

// reviewed by: Omar Khalifa