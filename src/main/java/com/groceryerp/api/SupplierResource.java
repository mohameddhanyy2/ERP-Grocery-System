package com.groceryerp.api;

import com.groceryerp.supplier.SupplierModule;
import com.groceryerp.supplier.SupplierRepository;
import com.groceryerp.supplier.beans.SupplierBean;
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
 * SupplierResource — JAX-RS resource that replaces the old SupplierServlet.
 *
 * The original servlet held its OWN raw SQL (cross-table JOINs against
 * purchase_orders/suppliers/stores/order_lines/products/stock_alerts/
 * supplier_products). Those joins now live in the injected @Stateless
 * {@link SupplierRepository}, so no SQL strings remain in the web layer.
 *
 *   GET  /api/supplier/suppliers              — all suppliers
 *   GET  /api/supplier/orders[?storeId|?supplierId] — purchase orders (+ names)
 *   GET  /api/supplier/orderlines?orderId=    — line items for one order
 *   GET  /api/supplier/status?orderId=        — status of one order
 *   GET  /api/supplier/stockalerts[?supplierId=] — low-stock alerts
 *   GET  /api/supplier/products?supplierId=   — products a supplier carries
 *   GET  /api/supplier/events?supplierId=     — SSE alert stream (supplier portal)
 *   POST /api/supplier/order                  — place a new order
 *   POST /api/supplier/quote                  — supplier submits a supply quote
 *   POST /api/supplier/accept                 — manager accepts (QUOTED→ACCEPTED)
 *   POST /api/supplier/outfordelivery         — supplier marks out for delivery
 *   POST /api/supplier/delivery               — manager marks delivered (updates stock)
 *   POST /api/supplier/add                    — add a supplier
 *   POST /api/supplier/products               — assign a product to a supplier
 *   POST /api/supplier/removeproduct          — remove a product from a supplier
 */
@Path("/supplier")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SupplierResource {

    @EJB
    private SupplierModule supplier;

    @EJB
    private SupplierRepository repository;

    // The SSE broadcaster is now a container-managed @Singleton EJB, injected
    // here instead of reached through the old hand-rolled getInstance() singleton.
    @EJB
    private AlertBroadcaster alertBroadcaster;

    // ── GET endpoints ─────────────────────────────────────────────

    @GET
    @Path("/suppliers")
    public List<Map<String, Object>> suppliers() {
        // Old SQL: SELECT * FROM suppliers ORDER BY name
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SupplierBean s : repository.findAllSuppliers()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("supplierId", s.getSupplierId());
            row.put("name", s.getName());
            row.put("contactEmail", s.getContactEmail());
            row.put("leadTimeDays", s.getLeadTimeDays());
            rows.add(row);
        }
        return rows;
    }

    @GET
    @Path("/orders")
    public List<Map<String, Object>> orders(@QueryParam("storeId") String storeId,
                                            @QueryParam("supplierId") String supplierId) {
        // Old SQL: the purchase_orders/suppliers/stores LEFT JOINs now live in the repository.
        return repository.findOrdersWithNames(storeId, supplierId);
    }

    @GET
    @Path("/orderlines")
    public Response orderlines(@QueryParam("orderId") String orderId) {
        if (orderId == null) {
            return Response.status(400).entity(Map.of("error", "orderId required")).build();
        }
        return Response.ok(repository.findOrderLinesWithProductName(orderId)).build();
    }

    @GET
    @Path("/status")
    public Response status(@QueryParam("orderId") String orderId) {
        if (orderId == null) {
            return Response.status(400).entity(Map.of("error", "orderId required")).build();
        }
        return Response.ok(Map.of("orderId", orderId, "status", supplier.getOrderStatus(orderId))).build();
    }

    @GET
    @Path("/stockalerts")
    public List<Map<String, Object>> stockalerts(@QueryParam("supplierId") String supplierId) {
        // Old SQL: stock_alerts joined to products/stores/supplier_products/purchase_orders.
        return repository.findStockAlerts(supplierId);
    }

    @GET
    @Path("/products")
    public Response products(@QueryParam("supplierId") String supplierId) {
        if (supplierId == null) {
            return Response.status(400).entity(Map.of("error", "supplierId required")).build();
        }
        return Response.ok(repository.findSupplierProducts(supplierId)).build();
    }

    /**
     * GET /api/supplier/lastprice?supplierId=&productId= — the last unit price this
     * supplier quoted for this product, used to pre-fill the quote form. Returns
     * { lastPrice: <number|null> }.
     */
    @GET
    @Path("/lastprice")
    public Response lastPrice(@QueryParam("supplierId") String supplierId,
                              @QueryParam("productId") String productId) {
        if (supplierId == null || productId == null) {
            return Response.status(400).entity(Map.of("error", "supplierId and productId required")).build();
        }
        Double last = repository.lastUnitPrice(supplierId, productId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("lastPrice", last); // may be null
        return Response.ok(body).build();
    }

    /**
     * GET /api/supplier/events?supplierId= — SSE stream pushing "alert" events to
     * the supplier portal. Kept faithful to the servlet version: it registers a
     * {@link PrintWriter} with the existing AlertBroadcaster. JAX-RS exposes the
     * underlying HttpServletResponse via @Context, so AlertBroadcaster stays
     * unchanged (PrintWriter-based).
     */
    @GET
    @Path("/events")
    @Produces("text/event-stream")
    public void events(@QueryParam("supplierId") String supplierId,
                       @Context HttpServletResponse resp) throws Exception {
        if (supplierId == null) {
            resp.sendError(400, "supplierId required");
            return;
        }
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("X-Accel-Buffering", "no");
        // CORS must be set HERE, not via CorsFilter: this method never returns
        // (it blocks holding the stream open), so the JAX-RS response filter never
        // runs and the cross-origin EventSource from :5174 would be blocked.
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.flushBuffer();

        PrintWriter writer = resp.getWriter();
        // Send a keep-alive comment immediately so the browser confirms the connection
        writer.write(": connected\n\n");
        writer.flush();

        alertBroadcaster.subscribe(supplierId, writer);
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
            alertBroadcaster.unsubscribe(supplierId, writer);
        }
    }

    /**
     * GET /api/supplier/manager-events — SSE stream for the manager UI (:5173).
     * A single shared channel; every order-lifecycle change is pushed here so the
     * manager's Supplier page refreshes live (quotes appearing, out-for-delivery,
     * etc.) without polling.
     */
    @GET
    @Path("/manager-events")
    @Produces("text/event-stream")
    public void managerEvents(@Context HttpServletResponse resp) throws Exception {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("X-Accel-Buffering", "no");
        // CORS set here for the same reason as /events: this method blocks and never
        // returns, so CorsFilter never runs for the SSE response.
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

    // ── POST endpoints ────────────────────────────────────────────

    @POST
    @Path("/order")
    public Response order(Map<String, Object> body) {
        try {
            String supplierId = (String) body.get("supplierId");
            String productId  = (String) body.get("productId");
            int quantity      = Integer.parseInt(body.get("quantity").toString());
            String storeId    = (String) body.get("storeId");
            String result     = supplier.placeOrder(supplierId, productId, quantity, storeId);
            return Response.ok(Map.of("result", result)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    @POST
    @Path("/delivery")
    public Response delivery(Map<String, Object> body) {
        try {
            String orderId   = (String) body.get("orderId");
            String productId = (String) body.get("productId");
            int quantity     = Integer.parseInt(body.get("quantity").toString());
            String result    = supplier.recordDelivery(orderId, productId, quantity);
            // Manager marked delivered → tell the owning supplier so it updates live.
            notifyOwningSupplier(orderId, "order");
            return Response.ok(Map.of("deliveryId", result)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    @POST
    @Path("/add")
    public Response add(Map<String, Object> body) {
        try {
            SupplierBean s = new SupplierBean();
            s.setSupplierId("SUP-" + System.currentTimeMillis());
            s.setName((String) body.get("name"));
            s.setContactEmail((String) body.getOrDefault("contactEmail", ""));
            s.setLeadTimeDays(Integer.parseInt(body.getOrDefault("leadTimeDays", "3").toString()));
            supplier.addSupplier(s);
            return Response.ok(Map.of("supplierId", s.getSupplierId(), "name", s.getName())).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    @POST
    @Path("/update")
    public Response updateSupplier(Map<String, Object> body) {
        try {
            String supplierId = (String) body.get("supplierId");
            String name = (String) body.get("name");
            String contactEmail = (String) body.get("contactEmail");
            int leadTimeDays = body.containsKey("leadTimeDays")
                    ? Integer.parseInt(body.get("leadTimeDays").toString()) : 0;
            repository.updateSupplier(supplierId, name, contactEmail, leadTimeDays);
            return Response.ok(Map.of("supplierId", supplierId)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/delete")
    public Response deleteSupplier(Map<String, Object> body) {
        try {
            String supplierId = (String) body.get("supplierId");
            if (repository.hasAnyOrders(supplierId)) {
                return Response.status(409).entity(Map.of("error",
                        "Cannot delete: supplier has purchase orders linked to it.")).build();
            }
            if (repository.hasAnyProducts(supplierId)) {
                return Response.status(409).entity(Map.of("error",
                        "Cannot delete: supplier still has products assigned. Remove them first.")).build();
            }
            repository.deleteSupplier(supplierId);
            return Response.ok(Map.of("deleted", supplierId)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /** POST /api/supplier/products — assign a product to a supplier. Body: { supplierId, productId } */
    @POST
    @Path("/products")
    public Response assignProduct(Map<String, Object> body) {
        try {
            String supplierId = (String) body.get("supplierId");
            String productId  = (String) body.get("productId");
            repository.assignSupplierProduct(supplierId, productId);
            return Response.ok(Map.of("supplierId", supplierId, "productId", productId)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    /** POST /api/supplier/removeproduct — remove a product from a supplier. Body: { supplierId, productId } */
    @POST
    @Path("/removeproduct")
    public Response removeProduct(Map<String, Object> body) {
        try {
            String supplierId = (String) body.get("supplierId");
            String productId  = (String) body.get("productId");
            repository.removeSupplierProduct(supplierId, productId);
            return Response.ok(Map.of("supplierId", supplierId, "productId", productId)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    /**
     * POST /api/supplier/quote — supplier submits a supply offer for an alert.
     * Body: { alertId, supplierId, quantity, unitPrice }. Creates a QUOTED purchase
     * order linked to the alert (via productAlertId) plus its single order line.
     */
    @POST
    @Path("/quote")
    public Response quote(Map<String, Object> body) {
        try {
            String alertId    = (String) body.get("alertId");
            String supplierId = (String) body.get("supplierId");
            int    quantity   = Integer.parseInt(body.get("quantity").toString());
            double unitPrice  = Double.parseDouble(body.get("unitPrice").toString());

            String[] alert = repository.findAlertProductAndStore(alertId);
            if (alert == null) {
                return Response.status(404).entity(Map.of("error", "Alert not found: " + alertId)).build();
            }
            String productId = alert[0];
            String storeId   = alert[1];

            String orderId = "ORD-" + System.currentTimeMillis();
            com.groceryerp.supplier.beans.PurchaseOrderBean order =
                    new com.groceryerp.supplier.beans.PurchaseOrderBean();
            order.setOrderId(orderId);
            order.setSupplierId(supplierId);
            order.setStoreId(storeId);
            order.setOrderDate(java.time.LocalDate.now().toString());
            order.setTotalCost(quantity * unitPrice);
            order.setStatus("QUOTED");
            order.setProductAlertId(alertId);
            repository.savePurchaseOrder(order);

            com.groceryerp.supplier.beans.OrderLineBean line =
                    new com.groceryerp.supplier.beans.OrderLineBean();
            line.setLineId("LINE-" + System.currentTimeMillis());
            line.setOrderId(orderId);
            line.setProductId(productId);
            line.setQuantity(quantity);
            line.setUnitPrice(unitPrice);
            repository.saveOrderLine(line);

            // Update the unit cost (purchase price) on the product and supplier_products.
            // The selling price (products.price) is intentionally NOT changed here —
            // the supplier's quote is a cost to us, not the price we charge customers.
            repository.updateProductUnitCost(supplierId, productId, unitPrice);

            // Supplier just quoted → tell the manager UI so the order appears live.
            alertBroadcaster.notifyManager("order", orderId);
            return Response.ok(Map.of("orderId", orderId, "status", "QUOTED")).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    /** POST /api/supplier/accept — manager accepts a QUOTED order → ACCEPTED. Body: { orderId } */
    @POST
    @Path("/accept")
    public Response accept(Map<String, Object> body) {
        try {
            String orderId = (String) body.get("orderId");
            int rows = repository.transitionOrderStatus(orderId, "QUOTED", "ACCEPTED");
            if (rows == 0) {
                return Response.status(409).entity(Map.of("error", "Order not found or not in QUOTED state")).build();
            }
            // Manager accepted → tell the owning supplier so it shows live on :5174.
            notifyOwningSupplier(orderId, "order");
            return Response.ok(Map.of("orderId", orderId, "status", "ACCEPTED")).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    /** POST /api/supplier/outfordelivery — supplier marks ACCEPTED order out for delivery. Body: { orderId } */
    @POST
    @Path("/outfordelivery")
    public Response outForDelivery(Map<String, Object> body) {
        try {
            String orderId = (String) body.get("orderId");
            int rows = repository.transitionOrderStatus(orderId, "ACCEPTED", "OUT_FOR_DELIVERY");
            if (rows == 0) {
                return Response.status(409).entity(Map.of("error", "Order not found or not in ACCEPTED state")).build();
            }
            // Supplier marked out for delivery → tell the manager UI live.
            alertBroadcaster.notifyManager("order", orderId);
            return Response.ok(Map.of("orderId", orderId, "status", "OUT_FOR_DELIVERY")).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", String.valueOf(e.getMessage()))).build();
        }
    }

    /**
     * Looks up the supplier that owns an order and pushes an SSE event to that
     * supplier's channel, so a manager-initiated change (accept / delivery) shows
     * up live on the supplier portal. No-op if the order can't be resolved.
     */
    private void notifyOwningSupplier(String orderId, String event) {
        var order = repository.findPurchaseOrderById(orderId);
        if (order != null && order.getSupplierId() != null) {
            alertBroadcaster.notifySupplier(order.getSupplierId(), event, orderId);
        }
    }
}
