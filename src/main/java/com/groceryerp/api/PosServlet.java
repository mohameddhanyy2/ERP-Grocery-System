package com.groceryerp.api;

import com.groceryerp.pos.POSModule;
import com.groceryerp.pos.beans.ReceiptBean;
import com.groceryerp.pos.beans.SaleBean;
import com.groceryerp.pos.beans.SaleItemBean;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * GET  /api/pos/sales              — all sales (newest first, limit 200)
 * GET  /api/pos/sales?storeId=     — sales for one store
 * GET  /api/pos/receipt?saleId=    — receipt text for a sale
 * POST /api/pos/sale               — process a new sale
 * POST /api/pos/discount           — apply discount to existing sale
 * POST /api/pos/payment            — process payment for a sale
 */
public class PosServlet extends BaseServlet {

    private final POSModule pos;

    public PosServlet(POSModule pos) {
        this.pos = pos;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "sales"   -> handleSales(req, resp);
            case "receipt" -> handleReceipt(req, resp);
            default -> error(resp, 404, "Unknown POS GET endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "sale"     -> handleSale(req, resp);
            case "discount" -> handleDiscount(req, resp);
            case "payment"  -> handlePayment(req, resp);
            default -> error(resp, 404, "Unknown POS POST endpoint");
        }
    }

    private void handleSales(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String storeId = req.getParameter("storeId");
        String sql = storeId != null
                ? "SELECT * FROM sales WHERE storeId=? ORDER BY timestamp DESC LIMIT 200"
                : "SELECT * FROM sales ORDER BY timestamp DESC LIMIT 200";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (storeId != null) ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("saleId", rs.getString("saleId"));
                    row.put("storeId", rs.getString("storeId"));
                    row.put("customerId", rs.getString("customerId"));
                    row.put("totalAmount", rs.getDouble("totalAmount"));
                    row.put("paymentMethod", rs.getString("paymentMethod"));
                    row.put("timestamp", rs.getString("timestamp"));
                    row.put("discountRate", rs.getDouble("discountRate"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            error(resp, 500, e.getMessage()); return;
        }
        json(resp, rows);
    }

    private void handleReceipt(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String saleId = req.getParameter("saleId");
        if (saleId == null) { error(resp, 400, "saleId required"); return; }
        json(resp, Map.of("receipt", pos.generateReceipt(saleId)));
    }

    @SuppressWarnings("unchecked")
    private void handleSale(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String storeId       = (String) body.getOrDefault("storeId", "STORE_A");
            String customerId    = (String) body.getOrDefault("customerId", "GUEST");
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "CASH");
            double amountPaid    = Double.parseDouble(body.getOrDefault("amountPaid", "0").toString());

            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
            if (rawItems == null || rawItems.isEmpty()) {
                error(resp, 400, "items array is required and must not be empty"); return;
            }

            List<SaleItemBean> items = new ArrayList<>();
            for (Map<String, Object> ri : rawItems) {
                SaleItemBean item = new SaleItemBean();
                item.setProductId((String) ri.get("productId"));
                item.setQuantity(Integer.parseInt(ri.get("quantity").toString()));
                items.add(item);
            }

            SaleBean sale = pos.processSale(items, storeId, customerId, paymentMethod, amountPaid);

            // Apply optional inline discount
            Object discountTypeRaw  = body.get("discountType");
            Object discountValueRaw = body.get("discountValue");
            if (discountTypeRaw != null && discountValueRaw != null) {
                double dv = Double.parseDouble(discountValueRaw.toString());
                if (dv > 0) pos.applyDiscount(sale, discountTypeRaw.toString(), dv);
            }

            // Build per-item detail for response
            List<Map<String, Object>> itemDetails = new ArrayList<>();
            for (SaleItemBean item : sale.getItems()) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("productId", item.getProductId());
                d.put("quantity", item.getQuantity());
                d.put("unitPrice", item.getUnitPrice());
                d.put("lineTotal", item.getLineTotal());
                itemDetails.add(d);
            }

            double tax       = Math.round(sale.getTotalAmount() * 0.14 * 100.0) / 100.0;
            double grandTotal = Math.round((sale.getTotalAmount() + tax) * 100.0) / 100.0;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("saleId", sale.getSaleId());
            result.put("storeId", sale.getStoreId());
            result.put("customerId", sale.getCustomerId());
            result.put("subtotal", sale.getTotalAmount());
            result.put("tax", tax);
            result.put("totalAmount", grandTotal);
            result.put("paymentMethod", sale.getPaymentMethod());
            result.put("timestamp", sale.getTimestamp());
            result.put("items", itemDetails);
            json(resp, result);
        } catch (IllegalStateException e) {
            error(resp, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            error(resp, 422, e.getMessage());
        } catch (Exception e) {
            error(resp, 400, e.getMessage());
        }
    }

    private void handleDiscount(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String saleId      = (String) body.get("saleId");
            String type        = (String) body.getOrDefault("discountType", "PERCENTAGE");
            double value       = Double.parseDouble(body.get("discountValue").toString());

            SaleBean.DAO dao = new SaleBean.DAO();
            SaleBean sale = dao.findById(saleId);
            if (sale == null) { error(resp, 404, "Sale not found: " + saleId); return; }
            pos.applyDiscount(sale, type, value);
            json(resp, Map.of("saleId", saleId, "newTotal", sale.getTotalAmount(), "discountType", type, "discountValue", value));
        } catch (Exception e) {
            error(resp, 400, e.getMessage());
        }
    }

    private void handlePayment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String saleId  = (String) body.get("saleId");
            double paid    = Double.parseDouble(body.get("amountPaid").toString());

            SaleBean.DAO dao = new SaleBean.DAO();
            SaleBean sale = dao.findById(saleId);
            if (sale == null) { error(resp, 404, "Sale not found: " + saleId); return; }
            ReceiptBean receipt = pos.processPayment(sale, paid);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("receiptId", receipt.getReceiptId());
            result.put("saleId", receipt.getSaleId());
            result.put("totalAmount", receipt.getTotalAmount());
            result.put("taxAmount", receipt.getTaxAmount());
            result.put("grandTotal", receipt.getGrandTotal());
            result.put("issuedAt", receipt.getIssuedAt());
            json(resp, result);
        } catch (IllegalArgumentException e) {
            error(resp, 422, e.getMessage());
        } catch (Exception e) {
            error(resp, 400, e.getMessage());
        }
    }
}
