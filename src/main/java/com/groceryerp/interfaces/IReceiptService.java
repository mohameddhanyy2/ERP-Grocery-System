package com.groceryerp.interfaces;

import jakarta.ejb.Local;
/** Provided by POSModule. Generates receipts for completed sales. */
@Local
public interface IReceiptService {
    String generateReceipt(String saleId);
}

// reviewed by: Omar Khalifa