package com.groceryerp.api;

import com.groceryerp.finance.FinanceModule;
import com.groceryerp.finance.beans.ExpenseBean;
import com.groceryerp.finance.beans.RevenueBean;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * GET  /api/finance/summary?period=   — revenue, expenses, net profit
 * GET  /api/finance/revenue           — all revenue rows
 * GET  /api/finance/expenses          — all expense rows
 * GET  /api/finance/tax               — all tax records
 * POST /api/finance/expense           — add an expense
 * POST /api/finance/revenue           — add a revenue entry
 */
public class FinanceServlet extends BaseServlet {

    private final FinanceModule finance;

    public FinanceServlet(FinanceModule finance) { this.finance = finance; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "summary"  -> handleSummary(req, resp);
            case "revenue"  -> handleRevenue(resp);
            case "expenses" -> handleExpenses(resp);
            case "tax"      -> handleTax(resp);
            default -> error(resp, 404, "Unknown finance GET endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "expense" -> handleAddExpense(req, resp);
            case "revenue" -> handleAddRevenue(req, resp);
            default -> error(resp, 404, "Unknown finance POST endpoint");
        }
    }

    private void handleSummary(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String period = param(req, "period", java.time.YearMonth.now().toString());
        double revenue  = finance.getTotalRevenue(period);
        double expenses = finance.getTotalExpenses(period);
        double profit   = finance.getNetProfit(period);
        json(resp, Map.of(
                "period", period,
                "totalRevenue", revenue,
                "totalExpenses", expenses,
                "netProfit", profit
        ));
    }

    private void handleRevenue(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM revenue ORDER BY period DESC LIMIT 200";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("revenueId", rs.getString("revenueId"));
                row.put("period", rs.getString("period"));
                row.put("storeId", rs.getString("storeId"));
                row.put("grossRevenue", rs.getDouble("grossRevenue"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleExpenses(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM expenses ORDER BY date DESC LIMIT 200";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("expenseId", rs.getString("expenseId"));
                row.put("storeId", rs.getString("storeId"));
                row.put("category", rs.getString("category"));
                row.put("amount", rs.getDouble("amount"));
                row.put("date", rs.getString("date"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleTax(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM tax_records ORDER BY period DESC LIMIT 100";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("taxId", rs.getString("taxId"));
                row.put("period", rs.getString("period"));
                row.put("taxRate", rs.getDouble("taxRate"));
                row.put("taxableAmount", rs.getDouble("taxableAmount"));
                row.put("taxOwed", rs.getDouble("taxOwed"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleAddExpense(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            ExpenseBean e = new ExpenseBean();
            e.setExpenseId("EXP-" + System.currentTimeMillis());
            e.setStoreId((String) body.get("storeId"));
            e.setCategory((String) body.getOrDefault("category", "MISC"));
            e.setAmount(Double.parseDouble(body.get("amount").toString()));
            e.setDate((String) body.getOrDefault("date", java.time.LocalDate.now().toString()));
            new ExpenseBean.DAO().save(e);
            json(resp, Map.of("expenseId", e.getExpenseId(), "amount", e.getAmount()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleAddRevenue(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            RevenueBean r = new RevenueBean();
            r.setRevenueId("REV-" + System.currentTimeMillis());
            r.setPeriod((String) body.get("period"));
            r.setStoreId((String) body.get("storeId"));
            r.setGrossRevenue(Double.parseDouble(body.get("grossRevenue").toString()));
            new RevenueBean.DAO().save(r);
            json(resp, Map.of("revenueId", r.getRevenueId(), "grossRevenue", r.getGrossRevenue()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }
}
