package com.groceryerp.api;

import com.groceryerp.inventory.beans.ProductBean;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * GET  /api/products/list             — all products
 * GET  /api/products/get?productId=   — single product
 * POST /api/products/add              — add a product
 * POST /api/products/update           — update price/name
 */
public class ProductServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "list" -> handleList(resp);
            case "get"  -> handleGet(req, resp);
            default     -> handleList(resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "add"    -> handleAdd(req, resp);
            case "update" -> handleUpdate(req, resp);
            default -> error(resp, 404, "Unknown products POST endpoint");
        }
    }

    private void handleList(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY category, name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("productId", rs.getString("productId"));
                row.put("name", rs.getString("name"));
                row.put("category", rs.getString("category"));
                row.put("price", rs.getDouble("price"));
                row.put("expiryDate", rs.getString("expiryDate"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String productId = req.getParameter("productId");
        if (productId == null) { error(resp, 400, "productId required"); return; }
        ProductBean p = new ProductBean.DAO().findById(productId);
        if (p == null) { error(resp, 404, "Product not found"); return; }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("productId", p.getProductId());
        row.put("name", p.getName());
        row.put("category", p.getCategory());
        row.put("price", p.getPrice());
        row.put("expiryDate", p.getExpiryDate());
        json(resp, row);
    }

    private void handleAdd(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            ProductBean p = new ProductBean();
            p.setProductId("PROD-" + System.currentTimeMillis());
            p.setName((String) body.get("name"));
            p.setCategory((String) body.getOrDefault("category", "General"));
            p.setPrice(Double.parseDouble(body.get("price").toString()));
            p.setExpiryDate((String) body.getOrDefault("expiryDate", ""));
            new ProductBean.DAO().save(p);
            json(resp, Map.of("productId", p.getProductId(), "name", p.getName(), "price", p.getPrice()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleUpdate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String productId = (String) body.get("productId");
            ProductBean p = new ProductBean.DAO().findById(productId);
            if (p == null) { error(resp, 404, "Product not found"); return; }
            if (body.containsKey("name"))       p.setName((String) body.get("name"));
            if (body.containsKey("category"))   p.setCategory((String) body.get("category"));
            if (body.containsKey("price"))      p.setPrice(Double.parseDouble(body.get("price").toString()));
            if (body.containsKey("expiryDate")) p.setExpiryDate((String) body.get("expiryDate"));
            new ProductBean.DAO().save(p);
            json(resp, Map.of("productId", p.getProductId(), "name", p.getName(), "price", p.getPrice()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }
}
