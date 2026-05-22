package com.groceryerp.integration;

import com.groceryerp.inventory.*;
import com.groceryerp.pos.*;
import com.groceryerp.supplier.*;
import com.groceryerp.hr.*;
import com.groceryerp.finance.*;
import com.groceryerp.customer.*;
import com.groceryerp.reporting.*;

/**
 * Main integration class — assembles the entire ERP system using IoC.
 *
 * This class is the ONLY place where modules are instantiated.
 * All required interface dependencies are wired here via setter injection.
 * No module creates another module internally — that is the IoC contract.
 *
 * Member 6 responsibility.
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("=== Grocery ERP System — Startup ===\n");

        // ── Step 1: Create foundation modules (no dependencies) ────
        HRModule hrModule = new HRModule();
        System.out.println("[IoC] HRModule instantiated (no required interfaces)");

        // ── Step 2: Create composite inventory with 3 store branches ─
        StoreInventoryBean storeA = new StoreInventoryBean();
        storeA.setStoreId("STORE_A");
        storeA.setStoreName("Cairo Branch");
        storeA.updateStock("PROD_001", 50);
        storeA.updateStock("PROD_002", 5);   // below threshold → alert

        StoreInventoryBean storeB = new StoreInventoryBean();
        storeB.setStoreId("STORE_B");
        storeB.setStoreName("Giza Branch");
        storeB.updateStock("PROD_001", 30);

        StoreInventoryBean storeC = new StoreInventoryBean();
        storeC.setStoreId("STORE_C");
        storeC.setStoreName("Alexandria Branch");
        storeC.updateStock("PROD_001", 20);

        CentralInventoryBean centralInventory = new CentralInventoryBean();
        centralInventory.addStore(storeA);  // IoC: stores injected
        centralInventory.addStore(storeB);
        centralInventory.addStore(storeC);
        System.out.println("[IoC] CentralInventoryBean assembled with 3 stores");

        // ── Step 3: Create and wire POS module ─────────────────────
        POSModule posModule = new POSModule();
        posModule.setStoreInventory(storeA);   // IoC: inject required interface
        System.out.println("[IoC] POSModule wired with IStoreInventory");

        // ── Step 4: Create and wire Supplier module ─────────────────
        SupplierModule supplierModule = new SupplierModule();
        supplierModule.setStoreInventory(storeA);       // IoC
        supplierModule.setStockAlerts(centralInventory); // IoC
        System.out.println("[IoC] SupplierModule wired with IStoreInventory + IStockAlerts");

        // ── Step 5: Create and wire Finance module ──────────────────
        FinanceModule financeModule = new FinanceModule();
        financeModule.setSalesData(posModule);       // IoC
        financeModule.setStaffData(hrModule);        // IoC
        financeModule.setOrderStatus(supplierModule); // IoC
        System.out.println("[IoC] FinanceModule wired with ISalesData + IStaffData + IOrderStatus");

        // ── Step 6: Create and wire Customer module ─────────────────
        CustomerModule customerModule = new CustomerModule();
        customerModule.setSalesData(posModule);  // IoC
        posModule.setCustomerData(customerModule); // IoC: back-wire customer into POS
        System.out.println("[IoC] CustomerModule wired with ISalesData");

        // ── Step 7: Create and wire Reporting module ─────────────────
        ReportingModule reportingModule = new ReportingModule();
        reportingModule.setSalesData(posModule);         // IoC
        reportingModule.setStaffData(hrModule);          // IoC
        reportingModule.setTotalStock(centralInventory); // IoC
        reportingModule.setFinanceData(financeModule);   // IoC
        reportingModule.setCustomerData(customerModule); // IoC
        System.out.println("[IoC] ReportingModule wired with all 5 required interfaces\n");

        // ── Step 8: Demo scenario ────────────────────────────────────
        System.out.println("=== Demo Scenario ===\n");

        // Check total stock across all stores
        int totalProd001 = centralInventory.getTotalStock("PROD_001");
        System.out.println("Total stock for PROD_001 across all stores: " + totalProd001);

        // Stores with low stock
        System.out.println("Stores with low stock: " + centralInventory.getStoresWithLowStock());

        // Process a sale
        posModule.processSale("PROD_001", 2, "STORE_A", "CUST_001");
        System.out.println("Sale processed at STORE_A for PROD_001 x2");

        // Generate a report
        String report = reportingModule.generateReport("SALES", "2025-05", "STORE_A");
        System.out.println("Report generated: " + report);

        System.out.println("\n=== ERP System Running ===");
    }
}
