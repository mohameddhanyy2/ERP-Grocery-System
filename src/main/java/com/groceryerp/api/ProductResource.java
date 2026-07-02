package com.groceryerp.api;

import com.groceryerp.inventory.InventoryRepository;
import com.groceryerp.inventory.beans.ProductBean;
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
 * ProductResource — JAX-RS resource that replaces the old ProductServlet.
 *
 * The original servlet held its OWN raw SQL ("SELECT * FROM products ...") and
 * called {@code new ProductBean.DAO()} directly. Persistence is now delegated to
 * the injected @Stateless {@link InventoryRepository}, so no SQL strings remain
 * in the web layer.
 *
 *   GET  /api/products/list             — all products
 *   GET  /api/products/get?productId=   — single product
 *   POST /api/products/add              — add a product
 *   POST /api/products/update           — update price/name/category/expiry
 */
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @EJB
    private InventoryRepository repository;

    @EJB
    private SupplierRepository supplierRepository;

    /**
     * productId -> comma-separated supplier name(s). The frontend fetches this once
     * per page (Inventory / POS / alerts) and shows the supplier next to each
     * product. Products with no supplier are simply absent from the map.
     */
    @GET
    @Path("/suppliers")
    public Map<String, String> suppliersByProduct() {
        return supplierRepository.productSupplierNames();
    }

    @GET
    @Path("/list")
    public List<Map<String, Object>> list() {
        // Old SQL: SELECT * FROM products ORDER BY category, name
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductBean p : repository.findAllProductsOrdered()) {
            rows.add(toRow(p));
        }
        return rows;
    }

    @GET
    @Path("/get")
    public Response get(@QueryParam("productId") String productId) {
        if (productId == null) {
            return Response.status(400).entity(Map.of("error", "productId required")).build();
        }
        ProductBean p = repository.findProductById(productId);
        if (p == null) {
            return Response.status(404).entity(Map.of("error", "Product not found")).build();
        }
        return Response.ok(toRow(p)).build();
    }

    @POST
    @Path("/add")
    public Response add(Map<String, Object> body) {
        try {
            String supplierId = (String) body.get("supplierId");
            if (supplierId == null || supplierId.isBlank()) {
                return Response.status(400).entity(Map.of("error", "supplierId is required")).build();
            }
            double unitCost = body.containsKey("unitCost")
                    ? Double.parseDouble(body.get("unitCost").toString()) : 0.0;
            double sellingPrice = body.containsKey("price")
                    ? Double.parseDouble(body.get("price").toString()) : unitCost;

            ProductBean p = new ProductBean();
            p.setProductId("PROD-" + System.currentTimeMillis());
            p.setName((String) body.get("name"));
            p.setCategory((String) body.getOrDefault("category", "General"));
            p.setPrice(sellingPrice);
            p.setUnitCost(unitCost);
            p.setSupplierId(supplierId);
            p.setBarcode((String) body.getOrDefault("barcode", ""));
            p.setExpiryDate((String) body.getOrDefault("expiryDate", ""));
            repository.saveProduct(p);
            supplierRepository.assignSupplierProduct(supplierId, p.getProductId(), unitCost);
            return Response.ok(Map.of(
                    "productId", p.getProductId(),
                    "name", p.getName(),
                    "price", p.getPrice(),
                    "unitCost", p.getUnitCost(),
                    "supplierId", p.getSupplierId())).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/update")
    public Response update(Map<String, Object> body) {
        try {
            String productId = (String) body.get("productId");
            ProductBean p = repository.findProductById(productId);
            if (p == null) {
                return Response.status(404).entity(Map.of("error", "Product not found")).build();
            }
            if (body.containsKey("name"))       { p.setName((String) body.get("name")); }
            if (body.containsKey("category"))   { p.setCategory((String) body.get("category")); }
            if (body.containsKey("price"))      { p.setPrice(Double.parseDouble(body.get("price").toString())); }
            if (body.containsKey("unitCost"))   { p.setUnitCost(Double.parseDouble(body.get("unitCost").toString())); }
            if (body.containsKey("barcode"))    { p.setBarcode((String) body.get("barcode")); }
            if (body.containsKey("expiryDate")) { p.setExpiryDate((String) body.get("expiryDate")); }
            repository.saveProduct(p);
            return Response.ok(Map.of(
                    "productId", p.getProductId(),
                    "name", p.getName(),
                    "price", p.getPrice())).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/delete")
    public Response delete(Map<String, Object> body) {
        try {
            String productId = (String) body.get("productId");
            ProductBean p = repository.findProductById(productId);
            if (p == null) {
                return Response.status(404).entity(Map.of("error", "Product not found")).build();
            }
            // Block if stock exists
            if (repository.hasAnyStock(productId)) {
                return Response.status(409).entity(Map.of("error",
                        "Cannot delete: product has stock in one or more stores.")).build();
            }
            // Block if orders exist
            if (repository.hasAnyOrders(productId)) {
                return Response.status(409).entity(Map.of("error",
                        "Cannot delete: product has purchase orders linked to it.")).build();
            }
            supplierRepository.removeAllSupplierProductLinks(productId);
            repository.deleteProduct(productId);
            return Response.ok(Map.of("deleted", productId)).build();
        } catch (Exception e) {
            return Response.status(400).entity(Map.of("error", e.getMessage())).build();
        }
    }

    private Map<String, Object> toRow(ProductBean p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("productId", p.getProductId());
        row.put("name", p.getName());
        row.put("category", p.getCategory());
        row.put("price", p.getPrice());
        row.put("unitCost", p.getUnitCost());
        row.put("barcode", p.getBarcode() != null ? p.getBarcode() : "");
        row.put("supplierId", p.getSupplierId() != null ? p.getSupplierId() : "");
        row.put("expiryDate", p.getExpiryDate());
        return row;
    }
}
