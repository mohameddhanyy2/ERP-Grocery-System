package com.groceryerp.interfaces;
import java.util.List;
/** Provided by CentralInventoryBean. Required by: SupplierModule. */
public interface IStockAlerts {
    boolean isRestockNeeded(String productId, String storeId);
    List<String> getProductsNeedingRestock(String storeId);
}
