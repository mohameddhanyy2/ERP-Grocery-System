package com.groceryerp.pos;

import com.groceryerp.interfaces.*;
import com.groceryerp.pos.beans.*;

/**
 * POS Module — Component implementation.
 *
 * PROVIDED interfaces: ISalesData, IReceiptService
 * REQUIRED interfaces: IStoreInventory, ICustomerData (injected via IoC)
 *
 * IoC: required dependencies are NEVER instantiated here.
 * They are injected from outside via setter methods.
 */
public class POSModule implements ISalesData, IReceiptService {

    // Required interfaces — injected via IoC (setter injection)
    private IStoreInventory storeInventory;
    private ICustomerData customerData;

    public POSModule() {}

    // ── IoC setters (required interfaces) ──────────────────────────
    public void setStoreInventory(IStoreInventory storeInventory) {
        this.storeInventory = storeInventory;
    }
    public void setCustomerData(ICustomerData customerData) {
        this.customerData = customerData;
    }

    // ── ISalesData (provided) ───────────────────────────────────────
    @Override
    public double getTotalRevenueBySale(String storeId, String date) {
        // TODO: implement — sum all SaleBeans for storeId on date
        return 0.0;
    }

    @Override
    public int getTransactionCount(String date) {
        // TODO: implement — count SaleBeans on date
        return 0;
    }

    // ── IReceiptService (provided) ──────────────────────────────────
    @Override
    public String generateReceipt(String saleId) {
        // TODO: implement — build receipt string from SaleBean
        return "RECEIPT-" + saleId;
    }

    /**
     * Core POS operation: process a sale.
     * Calls IStoreInventory.checkStock() before completing — IoC in action.
     */
    public SaleBean processSale(String productId, int quantity, String storeId, String customerId) {
        if (storeInventory.checkStock(productId) < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        storeInventory.updateStock(productId, -quantity);
        SaleBean sale = new SaleBean();
        sale.setSaleId("SALE-" + System.currentTimeMillis());
        sale.setStoreId(storeId);
        sale.setCustomerId(customerId);
        sale.setTimestamp(java.time.LocalDateTime.now().toString());
        return sale;
    }
}
