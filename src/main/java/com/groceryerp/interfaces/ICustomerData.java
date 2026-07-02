package com.groceryerp.interfaces;

import jakarta.ejb.Local;
import java.util.List;
/** Provided by CustomerModule. Required by: POSModule, ReportingModule. */
@Local
public interface ICustomerData {
    String getCustomerName(String customerId);
    List<String> getPurchaseHistoryIds(String customerId);
    double getTotalSpend(String customerId);
}

// reviewed by: Omar Khalifa