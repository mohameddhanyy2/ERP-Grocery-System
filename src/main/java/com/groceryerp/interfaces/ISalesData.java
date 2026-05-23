package com.groceryerp.interfaces;
import java.util.List;

/** Provided by POSModule. Required by: FinanceModule, CustomerModule, ReportingModule. */
public interface ISalesData {
    double getTotalRevenueBySale(String storeId, String date);
    int getTransactionCount(String date);
}

// reviewed by: Omar Khalifa
