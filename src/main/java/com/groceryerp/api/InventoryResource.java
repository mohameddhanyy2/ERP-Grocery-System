package com.groceryerp.api;

import com.groceryerp.inventory.CentralInventoryBean;
import com.groceryerp.inventory.InventoryRepository;
import com.groceryerp.inventory.beans.ProductBean;
import com.groceryerp.inventory.beans.StockAlertBean;
import com.groceryerp.supplier.SupplierRepository;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * InventoryResource — JAX-RS resource that replaces the old InventoryServlet.
 *
 * The original servlet was constructed with (central, stockAlertMDB) and held
 * its OWN raw SQL (stock / products / stock_alerts joins). The @Stateful
 * {@link CentralInventoryBean} is now injected via {@code @EJB}, and the raw SQL
 * moved into the @Stateless {@link InventoryRepository} so no JDBC strings
 * remain in the web layer.
 *
 *   GET  /api/inventory/stock              — all stock rows
 *   GET  /api/inventory/stock?storeId=     — stock for one store
 *   GET  /api/inventory/available?storeId= — products+qty for POS cart
 *   GET  /api/inventory/total?productId=   — chain-wide total
 *   GET  /api/inventory/lowstock           — stores with low stock
 *   GET  /api/inventory/alerts             — stock_alerts rows
 *   GET  /api/inventory/stores             — all stores from DB
 *   POST /api/inventory/restock            — body: storeId,productId,quantity
 *   POST /api/inventory/store              — body: storeId,storeName (create store)
 */
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @EJB
    private CentralInventoryBean central;

    @EJB
    private InventoryRepository repository;

    @EJB
    private SupplierRepository supplierRepository;

    @GET
    @Path("/stock")
    public List<Map<String, Object>> stock(@QueryParam("storeId") String storeId) {
        Map<String, String> supplierNames = supplierRepository.productSupplierNames();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductBean p : repository.findAllProducts()) {
            for (Map<String, String> s : repository.loadStores()) {
                String sid = s.get("storeId");
                if (storeId != null && !storeId.equals(sid)) { continue; }
                int qty = repository.getStockQuantity(sid, p.getProductId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("storeId", sid);
                row.put("storeName", s.get("storeName"));
                row.put("productId", p.getProductId());
                row.put("productName", p.getName());
                row.put("category", p.getCategory());
                row.put("price", p.getPrice());
                row.put("unitCost", p.getUnitCost());
                row.put("supplierId", p.getSupplierId() != null ? p.getSupplierId() : "");
                row.put("supplierName", supplierNames.getOrDefault(p.getProductId(), "—"));
                row.put("quantity", qty);
                rows.add(row);
            }
        }
        return rows;
    }

    /** Returns all products with their current stock at a given store. Used by POS cart. */
    @GET
    @Path("/available")
    public Response available(@QueryParam("storeId") String storeId) {
        if (storeId == null) {
            return Response.status(400).entity(Map.of("error", "storeId required")).build();
        }
        // Old SQL: products LEFT JOIN stock ... COALESCE(quantity,0).
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductBean p : repository.findAllProductsOrdered()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.getProductId());
            row.put("name", p.getName());
            row.put("category", p.getCategory());
            row.put("price", p.getPrice());
            row.put("quantity", repository.getStockQuantity(storeId, p.getProductId()));
            rows.add(row);
        }
        return Response.ok(rows).build();
    }

    @GET
    @Path("/total")
    public Response total(@QueryParam("productId") String productId) {
        if (productId == null) {
            return Response.status(400).entity(Map.of("error", "productId required")).build();
        }
        return Response.ok(Map.of(
                "productId", productId,
                "totalStock", central.getTotalStock(productId))).build();
    }

    @GET
    @Path("/lowstock")
    public List<String> lowstock() {
        return central.getStoresWithLowStock();
    }

    @GET
    @Path("/alerts")
    public List<Map<String, Object>> alerts() {
        // Old SQL: stock_alerts LEFT JOIN products/stores for display names.
        // Reproduced by resolving names through the product catalogue + store list.
        Map<String, String> storeNames = new LinkedHashMap<>();
        for (Map<String, String> s : repository.loadStores()) {
            storeNames.put(s.get("storeId"), s.get("storeName"));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, String> s : repository.loadStores()) {
            for (StockAlertBean a : repository.findAlertsByStore(s.get("storeId"))) {
                ProductBean p = repository.findProductById(a.getProductId());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("alertId", a.getAlertId());
                // productId here historically carries the display NAME; rawProductId
                // is the real id so the frontend can map it to a supplier.
                row.put("productId", p != null ? p.getName() : a.getProductId());
                row.put("rawProductId", a.getProductId());
                row.put("storeId", storeNames.getOrDefault(a.getStoreId(), a.getStoreId()));
                row.put("currentQty", a.getCurrentQty());
                row.put("threshold", a.getThreshold());
                row.put("alertDate", a.getAlertDate());
                rows.add(row);
            }
        }
        return rows;
    }

    @GET
    @Path("/stores")
    public List<Map<String, Object>> stores() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, String> s : repository.loadStores()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storeId", s.get("storeId"));
            m.put("storeName", s.get("storeName"));
            list.add(m);
        }
        return list;
    }

    @POST
    @Path("/store")
    public Response createStore(Map<String, Object> body) {
        String storeId   = (String) body.get("storeId");
        String storeName = (String) body.get("storeName");
        if (storeId == null || storeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "storeId required")).build();
        }
        if (storeName == null || storeName.isBlank()) {
            return Response.status(400).entity(Map.of("error", "storeName required")).build();
        }
        // Normalise: uppercase, spaces → underscores
        storeId = storeId.toUpperCase().replaceAll("\\s+", "_");
        if (!repository.saveStore(storeId, storeName)) {
            return Response.status(409).entity(Map.of("error", "Store ID already exists: " + storeId)).build();
        }
        // The store is now persisted. We do NOT manually register it on this
        // CentralInventoryBean instance: @Stateful means each injection point can
        // hold a different instance, so a store added here would be invisible to the
        // POS module's instance. Instead, CentralInventoryBean.@PostConstruct loads
        // all DB stores into every fresh instance, so the new store is picked up
        // wherever the chain view is used.
        return Response.ok(Map.of("storeId", storeId, "storeName", storeName)).build();
    }

    /**
     * GET /api/inventory/lookup?barcode=&storeId=
     * Used by the cashier portal barcode scanner. Returns product info + stock at the given store.
     */
    @GET
    @Path("/lookup")
    public Response lookup(@QueryParam("barcode") String barcode, @QueryParam("storeId") String storeId) {
        if (barcode == null || barcode.isBlank()) {
            return Response.status(400).entity(Map.of("error", "barcode required")).build();
        }
        if (storeId == null || storeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "storeId required")).build();
        }
        com.groceryerp.inventory.beans.ProductBean p = repository.findProductByBarcode(barcode);
        if (p == null) {
            return Response.status(404).entity(Map.of("error", "No product found with barcode: " + barcode)).build();
        }
        int qty = repository.getStockQuantity(storeId, p.getProductId());
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("productId",   p.getProductId());
        row.put("name",        p.getName());
        row.put("category",    p.getCategory());
        row.put("price",       p.getPrice());
        row.put("unitCost",    p.getUnitCost());
        row.put("barcode",     p.getBarcode() != null ? p.getBarcode() : "");
        row.put("supplierId",  p.getSupplierId() != null ? p.getSupplierId() : "");
        row.put("expiryDate",  p.getExpiryDate() != null ? p.getExpiryDate() : "");
        row.put("quantity",    qty);
        return Response.ok(row).build();
    }

    @POST
    @Path("/store/update")
    public Response updateStore(Map<String, Object> body) {
        String storeId   = (String) body.get("storeId");
        String storeName = (String) body.get("storeName");
        if (storeId == null || storeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "storeId required")).build();
        }
        if (storeName == null || storeName.isBlank()) {
            return Response.status(400).entity(Map.of("error", "storeName required")).build();
        }
        if (!repository.updateStoreName(storeId, storeName)) {
            return Response.status(404).entity(Map.of("error", "Store not found: " + storeId)).build();
        }
        return Response.ok(Map.of("storeId", storeId, "storeName", storeName)).build();
    }

    @POST
    @Path("/store/delete")
    public Response deleteStore(Map<String, Object> body) {
        String storeId = (String) body.get("storeId");
        if (storeId == null || storeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "storeId required")).build();
        }
        if (repository.hasAnySales(storeId)) {
            return Response.status(409).entity(Map.of("error",
                    "Cannot delete: store has sales history.")).build();
        }
        if (repository.hasAnyStockInStore(storeId)) {
            return Response.status(409).entity(Map.of("error",
                    "Cannot delete: store still has stock. Clear inventory first.")).build();
        }
        repository.deleteStore(storeId);
        return Response.ok(Map.of("deleted", storeId)).build();
    }

    @POST
    @Path("/restock")
    public Response restock(Map<String, Object> body) {
        String storeId   = (String) body.get("storeId");
        String productId = (String) body.get("productId");
        int quantity     = Integer.parseInt(body.get("quantity").toString());
        var store = central.getStore(storeId);
        if (store == null) {
            return Response.status(404).entity(Map.of("error", "Store not found: " + storeId)).build();
        }
        // Only allow restock if a DELIVERED order exists for this product at this store.
        if (!repository.hasDeliveredOrder(storeId, productId)) {
            return Response.status(400).entity(Map.of("error",
                    "No delivered order found for this product at this store. "
                    + "Place and confirm a supplier order first.")).build();
        }
        store.updateStock(productId, quantity);
        return Response.ok(Map.of(
                "status", "ok",
                "storeId", storeId,
                "productId", productId,
                "delta", quantity)).build();
    }
}
