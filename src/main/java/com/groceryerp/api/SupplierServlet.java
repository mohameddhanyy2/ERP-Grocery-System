package com.groceryerp.api;

import com.groceryerp.supplier.SupplierModule;
import com.groceryerp.supplier.beans.SupplierBean;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * GET  /api/supplier/suppliers        — all suppliers
 * GET  /api/supplier/orders           — all purchase orders
 * GET  /api/supplier/orders?storeId=  — orders by store
 * GET  /api/supplier/status?orderId=  — status of one order
 * POST /api/supplier/order            — place a new order
 * POST /api/supplier/delivery         — record a delivery
 * POST /api/supplier/add              — add a supplier
 */
public class SupplierServlet extends BaseServlet {

    private final SupplierModule supplier;

    public SupplierServlet(SupplierModule supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "suppliers"  -> handleSuppliers(resp);
            case "orders"     -> handleOrders(req, resp);
            case "orderlines" -> handleOrderLines(req, resp);
            case "status"     -> handleStatus(req, resp);
            default -> error(resp, 404, "Unknown supplier GET endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "order"    -> handleOrder(req, resp);
            case "delivery" -> handleDelivery(req, resp);
            case "add"      -> handleAdd(req, resp);
            default -> error(resp, 404, "Unknown supplier POST endpoint");
        }
    }

    private void handleSuppliers(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("supplierId", rs.getString("supplierId"));
                row.put("name", rs.getString("name"));
                row.put("contactEmail", rs.getString("contactEmail"));
                row.put("leadTimeDays", rs.getInt("leadTimeDays"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleOrders(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String storeId = req.getParameter("storeId");
        String sql = storeId != null
                ? "SELECT po.*, s.name as supplierName FROM purchase_orders po LEFT JOIN suppliers s ON po.supplierId=s.supplierId WHERE po.storeId=? ORDER BY po.orderDate DESC"
                : "SELECT po.*, s.name as supplierName FROM purchase_orders po LEFT JOIN suppliers s ON po.supplierId=s.supplierId ORDER BY po.orderDate DESC LIMIT 200";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (storeId != null) ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orderId", rs.getString("orderId"));
                    row.put("supplierId", rs.getString("supplierId"));
                    row.put("supplierName", rs.getString("supplierName"));
                    row.put("storeId", rs.getString("storeId"));
                    row.put("orderDate", rs.getString("orderDate"));
                    row.put("totalCost", rs.getDouble("totalCost"));
                    row.put("status", rs.getString("status"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleOrderLines(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String orderId = req.getParameter("orderId");
        if (orderId == null) { error(resp, 400, "orderId required"); return; }
        String sql = "SELECT ol.*, p.name as productName FROM order_lines ol LEFT JOIN products p ON ol.productId=p.productId WHERE ol.orderId=?";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("lineId", rs.getString("lineId"));
                    row.put("orderId", rs.getString("orderId"));
                    row.put("productId", rs.getString("productId"));
                    row.put("productName", rs.getString("productName"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("unitPrice", rs.getDouble("unitPrice"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String orderId = req.getParameter("orderId");
        if (orderId == null) { error(resp, 400, "orderId required"); return; }
        json(resp, Map.of("orderId", orderId, "status", supplier.getOrderStatus(orderId)));
    }

    private void handleOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String supplierId = (String) body.get("supplierId");
            String productId  = (String) body.get("productId");
            int quantity      = Integer.parseInt(body.get("quantity").toString());
            String storeId    = (String) body.get("storeId");
            String result     = supplier.placeOrder(supplierId, productId, quantity, storeId);
            json(resp, Map.of("result", result));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleDelivery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String orderId   = (String) body.get("orderId");
            String productId = (String) body.get("productId");
            int quantity     = Integer.parseInt(body.get("quantity").toString());
            String result    = supplier.recordDelivery(orderId, productId, quantity);
            json(resp, Map.of("deliveryId", result));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleAdd(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            SupplierBean s = new SupplierBean();
            s.setSupplierId("SUP-" + System.currentTimeMillis());
            s.setName((String) body.get("name"));
            s.setContactEmail((String) body.getOrDefault("contactEmail", ""));
            s.setLeadTimeDays(Integer.parseInt(body.getOrDefault("leadTimeDays", "3").toString()));
            supplier.addSupplier(s);
            json(resp, Map.of("supplierId", s.getSupplierId(), "name", s.getName()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }
}
