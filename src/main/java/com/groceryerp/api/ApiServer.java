package com.groceryerp.api;

import com.groceryerp.customer.CustomerModule;
import com.groceryerp.db.DatabaseManager;
import com.groceryerp.finance.FinanceModule;
import com.groceryerp.hr.HRModule;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.StockAlertMDB;
import com.groceryerp.inventory.StoreInventoryBean;
import com.groceryerp.pos.POSModule;
import com.groceryerp.reporting.ReportingModule;
import com.groceryerp.supplier.SupplierModule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import java.util.Map;

/**
 * Embedded Jetty HTTP server that exposes all ERP modules as JSON REST endpoints.
 * Instantiates and wires all modules (same IoC order as Main.java) then registers
 * one servlet per module domain.
 */
public class ApiServer {

    private final Server server;

    public ApiServer(int port,
                     HRModule hrModule,
                     StockAlertMDB stockAlertMDB,
                     CentralInventoryBean centralInventory,
                     SupplierModule supplierModule,
                     CustomerModule customerModule,
                     POSModule posModule,
                     FinanceModule financeModule,
                     ReportingModule reportingModule) {

        server = new Server(port);

        // Load persisted stores into the central inventory at startup
        for (Map<String, String> row : DatabaseManager.loadStores()) {
            StoreInventoryBean store = new StoreInventoryBean();
            store.setStoreId(row.get("storeId"));
            store.setStoreName(row.get("storeName"));
            store.setStockAlertMDB(stockAlertMDB);
            centralInventory.addStore(store);
        }

        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/api");

        // CORS filter
        ctx.addServlet(new ServletHolder(new CorsServlet()), "/*");

        // Module servlets
        ctx.addServlet(new ServletHolder(new InventoryServlet(centralInventory, stockAlertMDB)), "/inventory/*");
        posModule.setCentralInventory(centralInventory);
        ctx.addServlet(new ServletHolder(new PosServlet(posModule)), "/pos/*");
        ctx.addServlet(new ServletHolder(new SupplierServlet(supplierModule)), "/supplier/*");
        ctx.addServlet(new ServletHolder(new HrServlet(hrModule)), "/hr/*");
        ctx.addServlet(new ServletHolder(new FinanceServlet(financeModule)), "/finance/*");
        ctx.addServlet(new ServletHolder(new CustomerServlet(customerModule)), "/customer/*");
        ctx.addServlet(new ServletHolder(new ReportingServlet(reportingModule)), "/reporting/*");
        ctx.addServlet(new ServletHolder(new ProductServlet()), "/products/*");
        ctx.addServlet(new ServletHolder(new DashboardServlet(centralInventory, posModule, hrModule, financeModule, customerModule)), "/dashboard/*");
        ctx.addServlet(new ServletHolder(new BaseServlet() {
            @Override
            protected void doPost(jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse resp) throws java.io.IOException {
                DatabaseManager.resetDatabase();
                centralInventory.clearStores();
                json(resp, Map.of("status", "ok", "message", "Database reset successfully"));
            }
        }), "/reset");

        server.setHandler(ctx);
    }

    public void start() throws Exception {
        server.start();
        System.out.println("[API] REST server running on http://localhost:" + ((org.eclipse.jetty.server.ServerConnector) server.getConnectors()[0]).getPort());
    }

    public void join() throws InterruptedException {
        server.join();
    }
}
