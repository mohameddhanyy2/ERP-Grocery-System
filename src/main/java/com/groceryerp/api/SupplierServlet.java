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
 * GET  /api/supplier/suppliers              — all suppliers
 * GET  /api/supplier/orders                 — all purchase orders
 * GET  /api/supplier/orders?storeId=        — orders by store
 * GET  /api/supplier/orders?supplierId=     — orders for one supplier (portal)
 * GET  /api/supplier/orderlines?orderId=    — line items for one order
 * GET  /api/supplier/status?orderId=        — status of one order
 * GET  /api/supplier/stockalerts            — low-stock alerts (for supplier portal)
 * POST /api/supplier/order                  — place a new order
 * POST /api/supplier/quote                  — supplier submits a supply quote
 * POST /api/supplier/accept                 — manager accepts an order (PENDING→ACCEPTED)
 * POST /api/supplier/outfordelivery         — supplier marks order out for delivery
 * POST /api/supplier/delivery               — manager marks delivered (updates stock)
 * POST /api/supplier/add                    — add a supplier
 */
public class SupplierServlet extends BaseServlet {

    private final SupplierModule supplier;

    public SupplierServlet(SupplierModule supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "suppliers"   -> handleSuppliers(resp);
            case "orders"      -> handleOrders(req, resp);
            case "orderlines"  -> handleOrderLines(req, resp);
            case "status"      -> handleStatus(req, resp);
            case "stockalerts" -> handleStockAlerts(req, resp);
            case "events"      -> handleEvents(req, resp);
            case "products"    -> handleSupplierProducts(req, resp);
            default -> error(resp, 404, "Unknown supplier GET endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "order"          -> handleOrder(req, resp);
            case "quote"          -> handleQuote(req, resp);
            case "accept"         -> handleAccept(req, resp);
            case "outfordelivery" -> handleOutForDelivery(req, resp);
            case "delivery"       -> handleDelivery(req, resp);
            case "add"            -> handleAdd(req, resp);
            case "products"       -> handleAssignProduct(req, resp);
            case "removeproduct"  -> handleRemoveProduct(req, resp);
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
        String storeId    = req.getParameter("storeId");
        String supplierId = req.getParameter("supplierId");
        String base = "SELECT po.*, s.name as supplierName, st.storeName FROM purchase_orders po " +
                      "LEFT JOIN suppliers s ON po.supplierId=s.supplierId " +
                      "LEFT JOIN stores st ON po.storeId=st.storeId ";
        String sql;
        if (storeId != null)         sql = base + "WHERE po.storeId=? ORDER BY po.orderDate DESC";
        else if (supplierId != null)  sql = base + "WHERE po.supplierId=? ORDER BY po.orderDate DESC";
        else                          sql = base + "ORDER BY po.orderDate DESC LIMIT 200";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (storeId != null)        ps.setString(1, storeId);
            else if (supplierId != null) ps.setString(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orderId", rs.getString("orderId"));
                    row.put("supplierId", rs.getString("supplierId"));
                    row.put("supplierName", rs.getString("supplierName"));
                    row.put("storeId", rs.getString("storeId"));
                    row.put("storeName", rs.getString("storeName"));
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

    /** GET /api/supplier/events?supplierId= — SSE stream; pushes "alert" events to the supplier portal. */
    private void handleEvents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String supplierId = req.getParameter("supplierId");
        if (supplierId == null) { error(resp, 400, "supplierId required"); return; }

        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("X-Accel-Buffering", "no");
        resp.flushBuffer();

        java.io.PrintWriter writer = resp.getWriter();
        // Send a keep-alive comment immediately so the browser confirms the connection
        writer.write(": connected\n\n");
        writer.flush();

        AlertBroadcaster.getInstance().subscribe(supplierId, writer);
        try {
            // Hold the connection open; heartbeat every 20 s to prevent proxy timeouts
            while (!writer.checkError()) {
                Thread.sleep(20_000);
                writer.write(": keep-alive\n\n");
                writer.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            AlertBroadcaster.getInstance().unsubscribe(supplierId, writer);
        }
    }

    /** GET /api/supplier/stockalerts[?supplierId=] — low-stock alerts for the supplier portal. */
    private void handleStockAlerts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String supplierId = req.getParameter("supplierId");
        List<Map<String, Object>> rows = new ArrayList<>();

        // When supplierId is given, only return alerts for products that supplier carries
        // (and not already quoted by another supplier).
        String sql;
        if (supplierId != null) {
            sql = "SELECT sa.alertId, sa.productId, COALESCE(p.name, sa.productId) as productName, " +
                  "sa.storeId, COALESCE(st.storeName, sa.storeId) as storeName, " +
                  "sa.currentQty, sa.threshold, sa.alertDate, " +
                  "po.orderId, po.status as orderStatus, po.supplierId as assignedSupplierId " +
                  "FROM stock_alerts sa " +
                  "INNER JOIN supplier_products sp ON sp.productId = sa.productId AND sp.supplierId = ? " +
                  "LEFT JOIN products p ON sa.productId = p.productId " +
                  "LEFT JOIN stores st ON sa.storeId = st.storeId " +
                  "LEFT JOIN purchase_orders po ON po.productAlertId = sa.alertId " +
                  "WHERE po.orderId IS NULL OR po.supplierId = ? " +
                  "ORDER BY sa.alertDate DESC";
        } else {
            sql = "SELECT sa.alertId, sa.productId, COALESCE(p.name, sa.productId) as productName, " +
                  "sa.storeId, COALESCE(st.storeName, sa.storeId) as storeName, " +
                  "sa.currentQty, sa.threshold, sa.alertDate, " +
                  "po.orderId, po.status as orderStatus, po.supplierId as assignedSupplierId " +
                  "FROM stock_alerts sa " +
                  "LEFT JOIN products p ON sa.productId = p.productId " +
                  "LEFT JOIN stores st ON sa.storeId = st.storeId " +
                  "LEFT JOIN purchase_orders po ON po.productAlertId = sa.alertId " +
                  "ORDER BY sa.alertDate DESC";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (supplierId != null) { ps.setString(1, supplierId); ps.setString(2, supplierId); }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("alertId",           rs.getString("alertId"));
                    row.put("productId",         rs.getString("productId"));
                    row.put("productName",       rs.getString("productName"));
                    row.put("storeId",           rs.getString("storeId"));
                    row.put("storeName",         rs.getString("storeName"));
                    row.put("currentQty",        rs.getInt("currentQty"));
                    row.put("threshold",         rs.getInt("threshold"));
                    row.put("alertDate",         rs.getString("alertDate"));
                    row.put("orderId",           rs.getString("orderId"));
                    row.put("orderStatus",       rs.getString("orderStatus"));
                    row.put("assignedSupplierId",rs.getString("assignedSupplierId"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    /** GET /api/supplier/products?supplierId= — products a supplier carries. */
    private void handleSupplierProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String supplierId = req.getParameter("supplierId");
        if (supplierId == null) { error(resp, 400, "supplierId required"); return; }
        String sql = "SELECT sp.productId, COALESCE(p.name, sp.productId) as productName " +
                     "FROM supplier_products sp LEFT JOIN products p ON sp.productId = p.productId " +
                     "WHERE sp.supplierId = ? ORDER BY productName";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId",   rs.getString("productId"));
                    row.put("productName", rs.getString("productName"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    /** POST /api/supplier/products — assign a product to a supplier. Body: { supplierId, productId } */
    private void handleAssignProduct(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String supplierId = (String) body.get("supplierId");
            String productId  = (String) body.get("productId");
            String sql = "INSERT OR IGNORE INTO supplier_products (supplierId, productId) VALUES (?, ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, supplierId);
                ps.setString(2, productId);
                ps.executeUpdate();
            }
            json(resp, Map.of("supplierId", supplierId, "productId", productId));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    /** POST /api/supplier/removeproduct — remove a product from a supplier. Body: { supplierId, productId } */
    private void handleRemoveProduct(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String supplierId = (String) body.get("supplierId");
            String productId  = (String) body.get("productId");
            String sql = "DELETE FROM supplier_products WHERE supplierId = ? AND productId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, supplierId);
                ps.setString(2, productId);
                ps.executeUpdate();
            }
            json(resp, Map.of("supplierId", supplierId, "productId", productId));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    /** POST /api/supplier/quote — supplier submits a supply offer for an alert.
     *  Body: { alertId, supplierId, quantity, unitPrice }
     *  Creates a PENDING purchase order linked to the alert. */
    private void handleQuote(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String alertId    = (String) body.get("alertId");
            String supplierId = (String) body.get("supplierId");
            int    quantity   = Integer.parseInt(body.get("quantity").toString());
            double unitPrice  = Double.parseDouble(body.get("unitPrice").toString());

            // Look up the alert to get productId and storeId
            String productId = null, storeId = null;
            String alertSql = "SELECT productId, storeId FROM stock_alerts WHERE alertId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(alertSql)) {
                ps.setString(1, alertId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { productId = rs.getString("productId"); storeId = rs.getString("storeId"); }
                }
            }
            if (productId == null) { error(resp, 404, "Alert not found: " + alertId); return; }

            String orderId = "ORD-" + System.currentTimeMillis();
            String insertOrder = "INSERT INTO purchase_orders (orderId,supplierId,storeId,orderDate,totalCost,status,productAlertId) " +
                                 "VALUES (?,?,?,?,?,'QUOTED',?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertOrder)) {
                ps.setString(1, orderId);
                ps.setString(2, supplierId);
                ps.setString(3, storeId);
                ps.setString(4, java.time.LocalDate.now().toString());
                ps.setDouble(5, quantity * unitPrice);
                ps.setString(6, alertId);
                ps.executeUpdate();
            }
            String lineId = "LINE-" + System.currentTimeMillis();
            String insertLine = "INSERT INTO order_lines (lineId,orderId,productId,quantity,unitPrice) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertLine)) {
                ps.setString(1, lineId);
                ps.setString(2, orderId);
                ps.setString(3, productId);
                ps.setInt(4, quantity);
                ps.setDouble(5, unitPrice);
                ps.executeUpdate();
            }
            json(resp, Map.of("orderId", orderId, "status", "QUOTED"));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    /** POST /api/supplier/accept — manager accepts a QUOTED order → ACCEPTED.
     *  Body: { orderId } */
    private void handleAccept(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String orderId = (String) body.get("orderId");
            String updateSql = "UPDATE purchase_orders SET status='ACCEPTED' WHERE orderId=? AND status='QUOTED'";
            int rows;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, orderId);
                rows = ps.executeUpdate();
            }
            if (rows == 0) { error(resp, 409, "Order not found or not in QUOTED state"); return; }
            json(resp, Map.of("orderId", orderId, "status", "ACCEPTED"));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    /** POST /api/supplier/outfordelivery — supplier marks ACCEPTED order as out for delivery.
     *  Body: { orderId } */
    private void handleOutForDelivery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String orderId = (String) body.get("orderId");
            String updateSql = "UPDATE purchase_orders SET status='OUT_FOR_DELIVERY' WHERE orderId=? AND status='ACCEPTED'";
            int rows;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, orderId);
                rows = ps.executeUpdate();
            }
            if (rows == 0) { error(resp, 409, "Order not found or not in ACCEPTED state"); return; }
            json(resp, Map.of("orderId", orderId, "status", "OUT_FOR_DELIVERY"));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }
}
