package com.groceryerp.api;

import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.StockAlertMDB;
import com.groceryerp.inventory.StoreInventoryBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import com.groceryerp.db.DatabaseManager;

/**
 * GET  /api/inventory/stock            — all stock rows
 * GET  /api/inventory/stock?storeId=   — stock for one store
 * GET  /api/inventory/available?storeId= — products+qty for POS cart
 * GET  /api/inventory/total?productId= — chain-wide total
 * GET  /api/inventory/lowstock         — stores with low stock
 * GET  /api/inventory/alerts           — stock_alerts rows
 * GET  /api/inventory/stores           — all stores from DB
 * POST /api/inventory/restock          — body: storeId,productId,quantity
 * POST /api/inventory/store            — body: storeId,storeName  (create store)
 */
public class InventoryServlet extends BaseServlet {

    private final CentralInventoryBean central;
    private final StockAlertMDB stockAlertMDB;

    public InventoryServlet(CentralInventoryBean central, StockAlertMDB stockAlertMDB) {
        this.central = central;
        this.stockAlertMDB = stockAlertMDB;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = pathAction(req);
        switch (action) {
            case "stock"     -> handleStock(req, resp);
            case "available" -> handleAvailable(req, resp);
            case "total"     -> handleTotal(req, resp);
            case "lowstock"  -> json(resp, central.getStoresWithLowStock());
            case "alerts"    -> handleAlerts(resp);
            case "stores"    -> handleStores(resp);
            default -> error(resp, 404, "Unknown inventory endpoint: " + action);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "restock" -> handleRestock(req, resp);
            case "store"   -> handleCreateStore(req, resp);
            default -> error(resp, 404, "Unknown POST endpoint");
        }
    }

    private void handleStock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String storeId = req.getParameter("storeId");
        String sql = storeId != null
                ? "SELECT s.storeId, s.productId, s.quantity, p.name, p.category, p.price FROM stock s LEFT JOIN products p ON s.productId=p.productId WHERE s.storeId=?"
                : "SELECT s.storeId, s.productId, s.quantity, p.name, p.category, p.price FROM stock s LEFT JOIN products p ON s.productId=p.productId ORDER BY s.storeId, s.productId";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (storeId != null) ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("storeId", rs.getString("storeId"));
                    row.put("productId", rs.getString("productId"));
                    row.put("productName", rs.getString("name"));
                    row.put("category", rs.getString("category"));
                    row.put("price", rs.getDouble("price"));
                    row.put("quantity", rs.getInt("quantity"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            error(resp, 500, e.getMessage()); return;
        }
        json(resp, rows);
    }

    /** Returns all products with their current stock at a given store (quantity >= 0). Used by POS cart. */
    private void handleAvailable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String storeId = req.getParameter("storeId");
        if (storeId == null) { error(resp, 400, "storeId required"); return; }
        String sql = "SELECT p.productId, p.name, p.category, p.price, COALESCE(s.quantity,0) AS quantity " +
                     "FROM products p LEFT JOIN stock s ON p.productId=s.productId AND s.storeId=? " +
                     "ORDER BY p.category, p.name";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", rs.getString("productId"));
                    row.put("name", rs.getString("name"));
                    row.put("category", rs.getString("category"));
                    row.put("price", rs.getDouble("price"));
                    row.put("quantity", rs.getInt("quantity"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleTotal(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String productId = req.getParameter("productId");
        if (productId == null) { error(resp, 400, "productId required"); return; }
        json(resp, Map.of("productId", productId, "totalStock", central.getTotalStock(productId)));
    }

    private void handleAlerts(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT sa.*, COALESCE(p.name, sa.productId) as productName, COALESCE(st.storeName, sa.storeId) as storeName " +
                     "FROM stock_alerts sa " +
                     "LEFT JOIN products p ON sa.productId=p.productId " +
                     "LEFT JOIN stores st ON sa.storeId=st.storeId " +
                     "ORDER BY sa.alertDate DESC LIMIT 100";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("alertId", rs.getString("alertId"));
                row.put("productId", rs.getString("productName"));
                row.put("storeId", rs.getString("storeName"));
                row.put("currentQty", rs.getInt("currentQty"));
                row.put("threshold", rs.getInt("threshold"));
                row.put("alertDate", rs.getString("alertDate"));
                rows.add(row);
            }
        } catch (SQLException e) {
            error(resp, 500, e.getMessage()); return;
        }
        json(resp, rows);
    }

    private void handleStores(HttpServletResponse resp) throws IOException {
        List<Map<String, String>> raw = DatabaseManager.loadStores();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, String> s : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storeId", s.get("storeId"));
            m.put("storeName", s.get("storeName"));
            list.add(m);
        }
        json(resp, list);
    }

    private void handleCreateStore(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String storeId   = (String) body.get("storeId");
            String storeName = (String) body.get("storeName");
            if (storeId == null || storeId.isBlank()) { error(resp, 400, "storeId required"); return; }
            if (storeName == null || storeName.isBlank()) { error(resp, 400, "storeName required"); return; }
            // Normalise: uppercase, spaces → underscores
            storeId = storeId.toUpperCase().replaceAll("\\s+", "_");
            if (!DatabaseManager.saveStore(storeId, storeName)) {
                error(resp, 409, "Store ID already exists: " + storeId); return;
            }
            // Register in the live central inventory so it's usable immediately
            StoreInventoryBean store = new StoreInventoryBean();
            store.setStoreId(storeId);
            store.setStoreName(storeName);
            store.setStockAlertMDB(stockAlertMDB);
            central.addStore(store);
            json(resp, Map.of("storeId", storeId, "storeName", storeName));
        } catch (Exception e) {
            error(resp, 400, e.getMessage());
        }
    }

    private void handleRestock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String storeId   = (String) body.get("storeId");
            String productId = (String) body.get("productId");
            int quantity     = Integer.parseInt(body.get("quantity").toString());
            var store = central.getStore(storeId);
            if (store == null) { error(resp, 404, "Store not found: " + storeId); return; }

            // Only allow restock if a DELIVERED order exists for this product at this store
            String checkSql =
                "SELECT COUNT(*) FROM purchase_orders po " +
                "JOIN order_lines ol ON po.orderId = ol.orderId " +
                "WHERE po.storeId = ? AND ol.productId = ? AND po.status = 'DELIVERED'";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, storeId);
                ps.setString(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        error(resp, 400, "No delivered order found for this product at this store. Place and confirm a supplier order first.");
                        return;
                    }
                }
            }

            store.updateStock(productId, quantity);
            json(resp, Map.of("status", "ok", "storeId", storeId, "productId", productId, "delta", quantity));
        } catch (Exception e) {
            error(resp, 400, e.getMessage());
        }
    }
}
