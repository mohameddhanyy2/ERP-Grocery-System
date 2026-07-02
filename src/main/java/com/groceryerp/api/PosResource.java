package com.groceryerp.api;

import com.groceryerp.customer.CustomerRepository;
import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.pos.POSModule;
import com.groceryerp.pos.POSRepository;
import com.groceryerp.pos.beans.ReceiptBean;
import com.groceryerp.pos.beans.SaleBean;
import com.groceryerp.pos.beans.SaleItemBean;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PosResource — JAX-RS resource that replaces the old PosServlet.
 *
 * The original servlet held its OWN raw SQL (a LEFT JOIN of sales against
 * customers to resolve customerName). That join is reproduced here via the
 * injected @Stateless {@link POSRepository} (ordered/limited sales rows) plus a
 * Java-side lookup against {@link CustomerRepository}, so no SQL strings remain in
 * the web layer.
 *
 *   GET  /api/pos/sales              — all sales (newest first, limit 200)
 *   GET  /api/pos/sales?storeId=     — sales for one store
 *   GET  /api/pos/receipt?saleId=    — receipt text for a sale
 *   POST /api/pos/sale               — process a new sale
 *   POST /api/pos/discount           — apply discount to existing sale
 *   POST /api/pos/payment            — process payment for a sale
 */
@Path("/pos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PosResource {

    @EJB
    private POSModule pos;

    @EJB
    private POSRepository repository;

    @EJB
    private CustomerRepository customerRepository;

    @EJB
    private AlertBroadcaster alertBroadcaster;

    @GET
    @Path("/events")
    @Produces("text/event-stream")
    public void posEvents(@Context HttpServletResponse resp) throws Exception {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("X-Accel-Buffering", "no");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.flushBuffer();

        PrintWriter writer = resp.getWriter();
        writer.write(": connected\n\n");
        writer.flush();

        alertBroadcaster.subscribeManager(writer);
        try {
            while (!writer.checkError()) {
                Thread.sleep(20_000);
                writer.write(": keep-alive\n\n");
                writer.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            alertBroadcaster.unsubscribeManager(writer);
        }
    }

    @GET
    @Path("/sales")
    public List<Map<String, Object>> sales(@QueryParam("storeId") String storeId) {
        // Old SQL: SELECT s.*, COALESCE(c.name, s.customerId) AS customerName
        //          FROM sales s LEFT JOIN customers c ON s.customerId=c.customerId
        //          [WHERE s.storeId=?] ORDER BY s.timestamp DESC LIMIT 200
        // Reproduced by fetching the ordered/limited sales then joining the
        // customer name in Java (COALESCE(c.name, s.customerId) fallback preserved).
        List<SaleBean> saleRows = storeId != null
                ? repository.findRecentSalesByStore(storeId)
                : repository.findRecentSales();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SaleBean s : saleRows) {
            CustomerBean c = customerRepository.findCustomerById(s.getCustomerId());
            String customerName = (c != null && c.getName() != null) ? c.getName() : s.getCustomerId();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("saleId", s.getSaleId());
            row.put("storeId", s.getStoreId());
            row.put("customerId", s.getCustomerId());
            row.put("customerName", customerName);
            row.put("totalAmount", s.getTotalAmount());
            row.put("paymentMethod", s.getPaymentMethod());
            row.put("timestamp", s.getTimestamp());
            row.put("discountRate", s.getDiscountRate());
            rows.add(row);
        }
        return rows;
    }

    @GET
    @Path("/receipt")
    public Response receipt(@QueryParam("saleId") String saleId) {
        if (saleId == null) {
            return Response.status(400).entity(Map.of("error", "saleId required")).build();
        }
        return Response.ok(Map.of("receipt", pos.generateReceipt(saleId))).build();
    }

    @POST
    @Path("/sale")
    @SuppressWarnings("unchecked")
    public Response sale(Map<String, Object> body) {
        try {
            String storeId       = (String) body.getOrDefault("storeId", "STORE_A");
            String customerId    = (String) body.getOrDefault("customerId", "GUEST");
            String paymentMethod = (String) body.getOrDefault("paymentMethod", "CASH");
            double amountPaid    = Double.parseDouble(body.getOrDefault("amountPaid", "0").toString());

            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) body.get("items");
            if (rawItems == null || rawItems.isEmpty()) {
                return Response.status(400).entity(Map.of("error", "items array is required and must not be empty")).build();
            }

            List<SaleItemBean> items = new ArrayList<>();
            for (Map<String, Object> ri : rawItems) {
                SaleItemBean item = new SaleItemBean();
                item.setProductId((String) ri.get("productId"));
                item.setQuantity(Integer.parseInt(ri.get("quantity").toString()));
                items.add(item);
            }

            SaleBean sale = pos.processSale(items, storeId, customerId, paymentMethod, amountPaid);

            // Notify manager UI live (POS page, Inventory, Dashboard)
            alertBroadcaster.notifyManager("sale", sale.getSaleId());

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
            return Response.ok(result).build();
        } catch (IllegalStateException e) {
            return Response.status(409).entity(Map.of("error", e.getMessage())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/discount")
    public Response discount(Map<String, Object> body) {
        try {
            String saleId      = (String) body.get("saleId");
            String type        = (String) body.getOrDefault("discountType", "PERCENTAGE");
            double value       = Double.parseDouble(body.get("discountValue").toString());

            SaleBean sale = repository.findSaleById(saleId);
            if (sale == null) {
                return Response.status(404).entity(Map.of("error", "Sale not found: " + saleId)).build();
            }
            pos.applyDiscount(sale, type, value);
            return Response.ok(Map.of("saleId", saleId, "newTotal", sale.getTotalAmount(),
                    "discountType", type, "discountValue", value)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/payment")
    public Response payment(Map<String, Object> body) {
        try {
            String saleId  = (String) body.get("saleId");
            double paid    = Double.parseDouble(body.get("amountPaid").toString());

            SaleBean sale = repository.findSaleById(saleId);
            if (sale == null) {
                return Response.status(404).entity(Map.of("error", "Sale not found: " + saleId)).build();
            }
            ReceiptBean receipt = pos.processPayment(sale, paid);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("receiptId", receipt.getReceiptId());
            result.put("saleId", receipt.getSaleId());
            result.put("totalAmount", receipt.getTotalAmount());
            result.put("taxAmount", receipt.getTaxAmount());
            result.put("grandTotal", receipt.getGrandTotal());
            result.put("issuedAt", receipt.getIssuedAt());
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(422).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
