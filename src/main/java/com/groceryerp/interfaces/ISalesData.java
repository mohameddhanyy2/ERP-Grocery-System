package com.groceryerp.interfaces;

import jakarta.ejb.Local;

/** Provided by POSModule. Required by: FinanceModule, CustomerModule, ReportingModule. */
@Local
public interface ISalesData {
    double getTotalRevenueBySale(String storeId, String date);
    int getTransactionCount(String date);
    double getTotalSpendByCustomer(String customerId);
}

// reviewed by: Omar Khalifa
// conflicts resolved by: Omar Khalifa
