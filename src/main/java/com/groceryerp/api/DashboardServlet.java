package com.groceryerp.api;

import com.groceryerp.customer.CustomerModule;
import com.groceryerp.finance.FinanceModule;
import com.groceryerp.hr.HRModule;
import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.pos.POSModule;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.time.YearMonth;
import java.util.*;

/**
 * GET /api/dashboard/summary — KPI summary for the home dashboard
 * GET /api/dashboard/charts  — chart data (sales by day for last 30 days)
 */
public class DashboardServlet extends BaseServlet {

    private final CentralInventoryBean central;
    private final POSModule pos;
    private final HRModule hr;
    private final FinanceModule finance;
    private final CustomerModule customer;

    public DashboardServlet(CentralInventoryBean central, POSModule pos,
                            HRModule hr, FinanceModule finance, CustomerModule customer) {
        this.central = central;
        this.pos = pos;
        this.hr = hr;
        this.finance = finance;
        this.customer = customer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "summary" -> handleSummary(req, resp);
            case "charts"  -> handleCharts(resp);
            default -> handleSummary(req, resp);
        }
    }

    private void handleSummary(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String period = param(req, "period", YearMonth.now().toString());

        // Counts
        int totalProducts  = countTable("products");
        int totalCustomers = countTable("customers");
        int totalEmployees = countTable("employees");
        int totalSuppliers = countTable("suppliers");
        int totalOrders    = countTable("purchase_orders");

        // Today's sales
        String today = java.time.LocalDate.now().toString();
        int todayTx        = pos.getTransactionCount(today);
        double todayRevenue = pos.getTotalRevenueBySale("", today);

        // Finance
        double revenue  = finance.getTotalRevenue(period);
        double expenses = finance.getTotalExpenses(period);
        double profit   = finance.getNetProfit(period);

        // Low stock stores
        List<String> lowStockStores = central.getStoresWithLowStock();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("period", period);
        summary.put("totalProducts", totalProducts);
        summary.put("totalCustomers", totalCustomers);
        summary.put("totalEmployees", totalEmployees);
        summary.put("totalSuppliers", totalSuppliers);
        summary.put("totalOrders", totalOrders);
        summary.put("todayTransactions", todayTx);
        summary.put("todayRevenue", todayRevenue);
        summary.put("periodRevenue", revenue);
        summary.put("periodExpenses", expenses);
        summary.put("periodProfit", profit);
        summary.put("lowStockStores", lowStockStores);
        summary.put("lowStockCount", lowStockStores.size());

        json(resp, summary);
    }

    private void handleCharts(HttpServletResponse resp) throws IOException {
        // Sales by day for last 30 days
        List<Map<String, Object>> salesByDay = new ArrayList<>();
        String sql = "SELECT substr(timestamp,1,10) as day, COUNT(*) as txCount, SUM(totalAmount) as total " +
                     "FROM sales GROUP BY substr(timestamp,1,10) ORDER BY day DESC LIMIT 30";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("day", rs.getString("day"));
                row.put("transactions", rs.getInt("txCount"));
                row.put("revenue", rs.getDouble("total"));
                salesByDay.add(row);
            }
        } catch (SQLException e) {
            error(resp, 500, e.getMessage()); return;
        }
        Collections.reverse(salesByDay);

        // Revenue by store
        List<Map<String, Object>> byStore = new ArrayList<>();
        String storeSql = "SELECT storeId, COUNT(*) as txCount, SUM(totalAmount) as total FROM sales GROUP BY storeId";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(storeSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("storeId", rs.getString("storeId"));
                row.put("transactions", rs.getInt("txCount"));
                row.put("revenue", rs.getDouble("total"));
                byStore.add(row);
            }
        } catch (SQLException e) {
            error(resp, 500, e.getMessage()); return;
        }

        // Category stock
        List<Map<String, Object>> categoryStock = new ArrayList<>();
        String catSql = "SELECT p.category, SUM(s.quantity) as total FROM stock s JOIN products p ON s.productId=p.productId GROUP BY p.category";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(catSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", rs.getString("category"));
                row.put("quantity", rs.getInt("total"));
                categoryStock.add(row);
            }
        } catch (SQLException e) {
            error(resp, 500, e.getMessage()); return;
        }

        json(resp, Map.of(
                "salesByDay", salesByDay,
                "revenueByStore", byStore,
                "stockByCategory", categoryStock
        ));
    }

    private int countTable(String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Count failed for " + table + ": " + e.getMessage());
        }
        return 0;
    }
}
