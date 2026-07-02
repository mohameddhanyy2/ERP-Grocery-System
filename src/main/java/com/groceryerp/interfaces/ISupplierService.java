package com.groceryerp.interfaces;

import jakarta.ejb.Local;

import java.util.List;

/** Provided by SupplierModule. Places purchase orders with suppliers. */
@Local
public interface ISupplierService {
    String placeOrder(String supplierId, String productId, int quantity, String storeId);
    List<String> getAllSupplierIds();
}

// reviewed by: Omar Khalifa