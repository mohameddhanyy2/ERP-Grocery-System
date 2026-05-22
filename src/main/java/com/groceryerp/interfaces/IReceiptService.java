package com.groceryerp.interfaces;
/** Provided by POSModule. Generates receipts for completed sales. */
public interface IReceiptService {
    String generateReceipt(String saleId);
}
