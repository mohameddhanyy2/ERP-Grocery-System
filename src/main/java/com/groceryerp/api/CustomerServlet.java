package com.groceryerp.api;

import com.groceryerp.customer.CustomerModule;
import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * GET  /api/customer/customers        — all customers
 * GET  /api/customer/loyalty          — all loyalty records
 * GET  /api/customer/history?customerId= — purchase history
 * GET  /api/customer/points?customerId=  — loyalty points + tier
 * POST /api/customer/add              — add a customer
 * POST /api/customer/points           — add loyalty points
 */
public class CustomerServlet extends BaseServlet {

    private final CustomerModule customer;

    public CustomerServlet(CustomerModule customer) { this.customer = customer; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "customers" -> handleCustomers(resp);
            case "loyalty"   -> handleLoyalty(resp);
            case "history"   -> handleHistory(req, resp);
            case "points"    -> handlePoints(req, resp);
            default -> error(resp, 404, "Unknown customer GET endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "add"    -> handleAdd(req, resp);
            case "points" -> handleAddPoints(req, resp);
            default -> error(resp, 404, "Unknown customer POST endpoint");
        }
    }

    private void handleCustomers(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT c.*, l.points, l.tier FROM customers c LEFT JOIN loyalty l ON c.customerId=l.customerId ORDER BY c.name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("customerId", rs.getString("customerId"));
                row.put("name", rs.getString("name"));
                row.put("email", rs.getString("email"));
                row.put("registeredStoreId", rs.getString("registeredStoreId"));
                row.put("points", rs.getObject("points") != null ? rs.getInt("points") : 0);
                row.put("tier", rs.getString("tier") != null ? rs.getString("tier") : "BRONZE");
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleLoyalty(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT l.*, c.name FROM loyalty l LEFT JOIN customers c ON l.customerId=c.customerId ORDER BY l.points DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("customerId", rs.getString("customerId"));
                row.put("name", rs.getString("name"));
                row.put("points", rs.getInt("points"));
                row.put("tier", rs.getString("tier"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String customerId = req.getParameter("customerId");
        if (customerId == null) { error(resp, 400, "customerId required"); return; }
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT ph.*, s.storeId, s.paymentMethod FROM purchase_history ph LEFT JOIN sales s ON ph.saleId=s.saleId WHERE ph.customerId=? ORDER BY ph.date DESC LIMIT 100";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("historyId", rs.getString("historyId"));
                    row.put("customerId", rs.getString("customerId"));
                    row.put("saleId", rs.getString("saleId"));
                    row.put("date", rs.getString("date"));
                    row.put("amount", rs.getDouble("amount"));
                    row.put("storeId", rs.getString("storeId"));
                    row.put("paymentMethod", rs.getString("paymentMethod"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handlePoints(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String customerId = req.getParameter("customerId");
        if (customerId == null) { error(resp, 400, "customerId required"); return; }
        json(resp, Map.of(
                "customerId", customerId,
                "points", customer.getLoyaltyPoints(customerId),
                "tier", customer.getLoyaltyTier(customerId),
                "totalSpend", customer.getTotalSpend(customerId)
        ));
    }

    private void handleAdd(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            CustomerBean c = new CustomerBean();
            c.setCustomerId("CUST-" + System.currentTimeMillis());
            c.setName((String) body.get("name"));
            c.setEmail((String) body.getOrDefault("email", ""));
            c.setRegisteredStoreId((String) body.getOrDefault("registeredStoreId", "STORE_A"));
            new CustomerBean.DAO().save(c);
            json(resp, Map.of("customerId", c.getCustomerId(), "name", c.getName()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleAddPoints(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String customerId = (String) body.get("customerId");
            double amount     = Double.parseDouble(body.get("saleAmount").toString());
            customer.addLoyaltyPoints(customerId, amount);
            json(resp, Map.of("customerId", customerId, "points", customer.getLoyaltyPoints(customerId)));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }
}
