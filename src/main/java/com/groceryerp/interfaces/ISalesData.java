package com.groceryerp.interfaces;
/** Provided by POSModule. Required by: FinanceModule, CustomerModule, ReportingModule. */
public interface ISalesData {
    double getTotalRevenueBySale(String storeId, String date);
    int getTransactionCount(String date);
}
