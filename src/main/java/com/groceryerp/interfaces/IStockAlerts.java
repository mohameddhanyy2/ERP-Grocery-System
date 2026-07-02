package com.groceryerp.interfaces;

import jakarta.ejb.Local;
import java.util.List;
/** Provided by CentralInventoryBean. Required by: SupplierModule. */
@Local
public interface IStockAlerts {
    boolean isRestockNeeded(String productId, String storeId);
    List<String> getProductsNeedingRestock(String storeId);
    void resolveRestockAlert(String productId, String storeId);
}

// reviewed by: Omar Khalifa