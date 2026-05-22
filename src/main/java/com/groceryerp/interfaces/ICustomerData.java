package com.groceryerp.interfaces;
import java.util.List;
/** Provided by CustomerModule. Required by: POSModule, ReportingModule. */
public interface ICustomerData {
    String getCustomerName(String customerId);
    List<String> getPurchaseHistoryIds(String customerId);
}
