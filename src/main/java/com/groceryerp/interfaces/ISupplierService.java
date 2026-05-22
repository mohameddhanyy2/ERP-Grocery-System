package com.groceryerp.interfaces;

import java.util.List;

/** Provided by SupplierModule. Places purchase orders with suppliers. */
public interface ISupplierService {
    String placeOrder(String supplierId, String productId, int quantity, String storeId);
    List<String> getAllSupplierIds();
}
