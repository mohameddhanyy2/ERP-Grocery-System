package com.groceryerp.interfaces;
import java.util.List;
/** Provided by CentralInventoryBean (composite). Required by: ReportingModule. */
public interface ITotalStock {
    int getTotalStock(String productId);
    List<String> getStoresWithLowStock();
    void redistributeStock(String fromStoreId, String toStoreId, String productId, int qty);
}

// reviewed by: Omar Khalifa