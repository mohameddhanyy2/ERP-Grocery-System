package com.groceryerp.api;

import com.groceryerp.customer.CustomerModule;
import com.groceryerp.finance.FinanceModule;
import com.groceryerp.hr.HRModule;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.pos.POSModule;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardResource — JAX-RS replacement for DashboardServlet. The five module
 * dependencies that used to be constructor-injected in ApiServer are now
 * container-injected via @EJB. The servlet's raw aggregate SQL has moved into
 * {@link DashboardRepository}.
 *
 *   GET /api/dashboard/summary — KPI summary
 *   GET /api/dashboard/charts  — chart data
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @EJB private CentralInventoryBean central;
    @EJB private POSModule pos;
    @EJB private HRModule hr;
    @EJB private FinanceModule finance;
    @EJB private CustomerModule customer;
    @EJB private DashboardRepository repository;

    @GET
    @Path("/summary")
    public Map<String, Object> summary(@QueryParam("period") String periodParam) {
        String period = periodParam != null ? periodParam : YearMonth.now().toString();

        String today = LocalDate.now().toString();
        int todayTx = pos.getTransactionCount(today);
        double todayRevenue = pos.getTotalRevenueBySale("", today);

        double revenue = finance.getTotalRevenue(period);
        double expenses = finance.getTotalExpenses(period);
        double profit = finance.getNetProfit(period);

        List<String> lowStockIds = central.getStoresWithLowStock();
        Map<String, String> nameMap = repository.storeNames();
        List<String> lowStockStores = new ArrayList<>();
        for (String id : lowStockIds) { lowStockStores.add(nameMap.getOrDefault(id, id)); }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("period", period);
        summary.put("totalProducts", repository.countTable("products"));
        summary.put("totalCustomers", repository.countTable("customers"));
        summary.put("totalEmployees", repository.countTable("employees"));
        summary.put("totalSuppliers", repository.countTable("suppliers"));
        summary.put("totalOrders", repository.countTable("purchase_orders"));
        summary.put("todayTransactions", todayTx);
        summary.put("todayRevenue", todayRevenue);
        summary.put("periodRevenue", revenue);
        summary.put("periodExpenses", expenses);
        summary.put("periodProfit", profit);
        summary.put("lowStockStores", lowStockStores);
        summary.put("lowStockCount", lowStockStores.size());
        return summary;
    }

    @GET
    @Path("/charts")
    public Map<String, Object> charts() {
        List<Map<String, Object>> salesByDay = repository.salesByDay();
        java.util.Collections.reverse(salesByDay);
        return Map.of(
                "salesByDay", salesByDay,
                "revenueByStore", repository.revenueByStore(),
                "stockByCategory", repository.stockByCategory()
        );
    }
}
