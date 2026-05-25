package com.groceryerp.integration;

import com.groceryerp.api.ApiServer;
import com.groceryerp.customer.CustomerModule;
import com.groceryerp.db.DatabaseManager;
import com.groceryerp.finance.FinanceModule;
import com.groceryerp.hr.HRModule;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.StockAlertMDB;
import com.groceryerp.pos.POSModule;
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

        // ── 3b. Central inventory — stores are loaded from DB by ApiServer ──
        CentralInventoryBean centralInventory = new CentralInventoryBean();
        System.out.println("[IoC] CentralInventoryBean instantiated (@Stateful) — stores loaded from DB at server start");

        // ── 4. Supplier — requires inventory interfaces ─────────────
        SupplierModule supplierModule = new SupplierModule();
        supplierModule.setCentralInventory(centralInventory);
        supplierModule.setStockAlerts(centralInventory);
        System.out.println("[IoC] SupplierModule wired (@Stateless)");

        // ── 5. Customer and POS — mutual back-wire ──────────────────
        CustomerModule customerModule = new CustomerModule();
        POSModule posModule = new POSModule();
        posModule.setCustomerData(customerModule);
        posModule.setLoyaltyService(customerModule);
        customerModule.setSalesData(posModule);
        System.out.println("[IoC] POSModule + CustomerModule wired (@Stateless)");

        // ── 7. Finance — requires sales, staff, orders ─────────────
        FinanceModule financeModule = new FinanceModule();
        financeModule.setSalesData(posModule);
        financeModule.setStaffData(hrModule);
        financeModule.setOrderStatus(supplierModule);
        System.out.println("[IoC] FinanceModule wired (@Stateless)");

        // ── 8. Reporting — requires all five interfaces ─────────────
        ReportingModule reportingModule = new ReportingModule();
        reportingModule.setSalesData(posModule);
        reportingModule.setStaffData(hrModule);
        reportingModule.setTotalStock(centralInventory);
        reportingModule.setFinanceData(financeModule);
        reportingModule.setCustomerData(customerModule);
        System.out.println("[IoC] ReportingModule wired with all 5 interfaces (@Stateless)\n");

        // ── 9. Start REST API server ────────────────────────────────
        try {
            ApiServer api = new ApiServer(8080,
                    hrModule, stockAlertMDB,
                    centralInventory, supplierModule,
                    customerModule, posModule,
                    financeModule, reportingModule);
            api.start();
            System.out.println("[API] Server started — press Ctrl+C to stop.\n");
            api.join();
        } catch (Exception e) {
            System.out.println("[API] Failed to start server: " + e.getMessage());
        }

    }
}
