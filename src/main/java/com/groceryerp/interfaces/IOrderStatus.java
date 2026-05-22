package com.groceryerp.interfaces;
import java.util.List;
/** Provided by SupplierModule. Required by: FinanceModule, ReportingModule. */
public interface IOrderStatus {
    String getOrderStatus(String orderId);
    double getTotalPurchaseCost(String period);
    List<String> getOrderIdsByStore(String storeId);
}
