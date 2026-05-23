package com.groceryerp.interfaces;
import java.util.List;
/** Provided by CentralInventoryBean. Required by: POSModule, SupplierModule. */
public interface IStoreInventory {
    int checkStock(String productId);
    void updateStock(String productId, int quantity);
    List<String> getLowStockAlerts();
    String getStoreId();
}

// reviewed by: Omar Khalifa
