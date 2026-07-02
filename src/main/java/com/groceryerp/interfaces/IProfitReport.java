package com.groceryerp.interfaces;

import jakarta.ejb.Local;
/** Provided by FinanceModule. Returns profit summary per store per period. */
@Local
public interface IProfitReport {
    String calcProfitSummary(String storeId, String period);
    double calcProfit();
}

// reviewed by: Omar Khalifa