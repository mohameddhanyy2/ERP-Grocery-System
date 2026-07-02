package com.groceryerp.interfaces;

import jakarta.ejb.Local;
import java.util.List;
/** Provided by SupplierModule. Required by: FinanceModule, ReportingModule. */
@Local
public interface IOrderStatus {
    String getOrderStatus(String orderId);
    double getTotalPurchaseCost(String period);
    List<String> getOrderIdsByStore(String storeId);
}

// reviewed by: Omar Khalifa