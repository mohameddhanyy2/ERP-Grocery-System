package com.groceryerp.integration;

import com.groceryerp.customer.CustomerModule;
import com.groceryerp.db.DatabaseManager;
import com.groceryerp.finance.FinanceModule;
import com.groceryerp.hr.HRModule;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.StockAlertMDB;
import com.groceryerp.inventory.StoreInventoryBean;
import com.groceryerp.pos.POSModule;
import com.groceryerp.pos.beans.ReceiptBean;
import com.groceryerp.pos.beans.SaleBean;
import com.groceryerp.reporting.ReportingModule;
import com.groceryerp.supplier.SupplierModule;

/**
 * Main — IoC assembler for the Grocery ERP system.
 *
 * This is the ONLY class that instantiates modules and wires dependencies.
 * No module creates another module internally — that is the IoC contract.
 * All wiring uses setter injection following the exact dependency order below.
 */
public class Main {

    public static void main(String[] args) {

        // ── 1. Initialize database and seed reference data ─────────
        DatabaseManager.initializeDatabase();

        System.out.println("\n=== Grocery ERP System — Startup ===\n");

        // ── 2. Foundation module — no dependencies ──────────────────
        HRModule hrModule = new HRModule();
        System.out.println("[IoC] HRModule instantiated (@Stateful)");

        // ── 3. Message-Driven Bean — created before stores ──────────
        StockAlertMDB stockAlertMDB = new StockAlertMDB();
        System.out.println("[IoC] StockAlertMDB instantiated (@MessageDriven)");

        // ── 3b. Stateful store beans — session configured via setters
        StoreInventoryBean storeA = new StoreInventoryBean();
        storeA.setStoreId("STORE_A");
        storeA.setStoreName("Cairo Branch");
        storeA.setStockAlertMDB(stockAlertMDB);       // IoC — inject MDB

        StoreInventoryBean storeB = new StoreInventoryBean();
        storeB.setStoreId("STORE_B");
        storeB.setStoreName("Giza Branch");
        storeB.setStockAlertMDB(stockAlertMDB);       // IoC

        StoreInventoryBean storeC = new StoreInventoryBean();
        storeC.setStoreId("STORE_C");
        storeC.setStoreName("Alexandria Branch");
        storeC.setStockAlertMDB(stockAlertMDB);       // IoC

        CentralInventoryBean centralInventory = new CentralInventoryBean();
        centralInventory.addStore(storeA);
        centralInventory.addStore(storeB);
        centralInventory.addStore(storeC);
        System.out.println("[IoC] CentralInventoryBean assembled with 3 stores (@Stateful)");

        // ── 4. Supplier — requires inventory interfaces ─────────────
        SupplierModule supplierModule = new SupplierModule();
        supplierModule.setStoreInventory(storeA);         // IoC
        supplierModule.setStockAlerts(centralInventory);  // IoC
        System.out.println("[IoC] SupplierModule wired (@Stateless)");

        // ── 5. Wire MDB now that supplier exists ────────────────────
        stockAlertMDB.setSupplierService(supplierModule); // IoC
        System.out.println("[IoC] StockAlertMDB wired with ISupplierService");

        // ── 6. Customer and POS — mutual back-wire ──────────────────
        CustomerModule customerModule = new CustomerModule();
        POSModule posModule = new POSModule();
        posModule.setStoreInventory(storeA);              // IoC
        posModule.setCustomerData(customerModule);        // IoC
        customerModule.setSalesData(posModule);           // IoC — back-wire
        System.out.println("[IoC] POSModule + CustomerModule wired (@Stateless)");

        // ── 7. Finance — requires sales, staff, orders ─────────────
        FinanceModule financeModule = new FinanceModule();
        financeModule.setSalesData(posModule);            // IoC
        financeModule.setStaffData(hrModule);             // IoC
        financeModule.setOrderStatus(supplierModule);     // IoC
        System.out.println("[IoC] FinanceModule wired (@Stateless)");

        // ── 8. Reporting — requires all five interfaces ─────────────
        ReportingModule reportingModule = new ReportingModule();
        reportingModule.setSalesData(posModule);          // IoC
        reportingModule.setStaffData(hrModule);           // IoC
        reportingModule.setTotalStock(centralInventory);  // IoC
        reportingModule.setFinanceData(financeModule);    // IoC
        reportingModule.setCustomerData(customerModule);  // IoC
        System.out.println("[IoC] ReportingModule wired with all 5 interfaces (@Stateless)\n");

        // ── Demo scenario ────────────────────────────────────────────
        System.out.println("=== Demo Scenario ===\n");

        // 1. Total stock across all stores
        int totalProd001 = centralInventory.getTotalStock("PROD_001");
        System.out.println("1. Total stock for PROD_001 across all stores: " + totalProd001);

        // 2. Stores with low stock
        System.out.println("2. Stores with low stock: " + centralInventory.getStoresWithLowStock());

        // 3. Process a sale (triggers MDB if stock drops below threshold after deduction)
        SaleBean sale = posModule.processSale("PROD_001", 2, "STORE_A", "CUST_001", "CASH", 50.0);
        System.out.println("3. Sale processed: " + sale.getSaleId() + " | Total: " + sale.getTotalAmount());

        // 4. Apply a 10% discount
        posModule.applyDiscount(sale, "PERCENTAGE", 10.0);
        System.out.println("4. Discount applied — new total: " + sale.getTotalAmount());

        // 5. Process payment
        ReceiptBean receipt = posModule.processPayment(sale, 100.0);
        System.out.println("5. Payment processed | Grand Total: " + receipt.getGrandTotal()
                + " | Change: " + (100.0 - receipt.getGrandTotal()));

        // 6. Generate receipt from DB
        String receiptText = posModule.generateReceipt(sale.getSaleId());
        System.out.println("6. Receipt:\n" + receiptText);

        // 7. Add loyalty points
        customerModule.addLoyaltyPoints("CUST_001", sale.getTotalAmount());
        System.out.println("7. Loyalty points for CUST_001: " + customerModule.getLoyaltyPoints("CUST_001")
                + " | Tier: " + customerModule.getLoyaltyTier("CUST_001"));

        // 8. Demonstrate HRModule Stateful payroll session
        System.out.println("8. Staff at STORE_A: " + hrModule.getStaffCount("STORE_A"));
        hrModule.beginPayrollSession("EMP_A2");
        hrModule.loadShifts("2026-05");
        hrModule.computeGrossPay();
        hrModule.endSession();
        System.out.println("   Payroll session for EMP_A2 completed (no shifts seeded → 0.0)");

        // 9. Finance and reporting
        String report = reportingModule.generateReport("SALES", "2026-05", "STORE_A");
        System.out.println("9. Report: " + report);

        System.out.println("\n=== ERP System Demo Complete ===");
    }
}
