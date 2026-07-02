package com.groceryerp.api;

import com.groceryerp.customer.CustomerModule;
import com.groceryerp.customer.CustomerRepository;
import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.customer.beans.LoyaltyBean;
import com.groceryerp.customer.beans.PurchaseHistoryBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomerResource — JAX-RS resource that replaces the old CustomerServlet.
 *
 * The original servlet held its OWN raw SQL (cross-table JOINs against
 * customers/loyalty/purchase_history/sales), bypassing the module. Those joins
 * are reproduced here via the injected @Stateless {@link CustomerRepository}
 * plus JPQL, so no SQL strings remain in the web layer.
 *
 *   GET  /api/customer/customers              — all customers + loyalty
 *   GET  /api/customer/loyalty                — all loyalty records
 *   GET  /api/customer/history?customerId=    — purchase history
 *   GET  /api/customer/points?customerId=     — points + tier + total spend
 *   POST /api/customer/add                    — add a customer
 *   POST /api/customer/points                 — add loyalty points
 */
@Path("/customer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @EJB
    private CustomerModule customer;

    @EJB
    private CustomerRepository repository;

    @GET
    @Path("/customers")
    public List<Map<String, Object>> customers() {
        // Old SQL: SELECT c.*, l.points, l.tier FROM customers c
        //          LEFT JOIN loyalty l ON c.customerId=l.customerId ORDER BY c.name
        // Reproduced by joining the two entity lists in Java.
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CustomerBean c : repository.findAllCustomers()) {
            LoyaltyBean l = repository.findLoyaltyByCustomerId(c.getCustomerId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerId", c.getCustomerId());
            row.put("name", c.getName());
            row.put("email", c.getEmail());
            row.put("registeredStoreId", c.getRegisteredStoreId());
            row.put("points", l != null ? l.getPoints() : 0);
            row.put("tier", l != null ? l.getTier() : "BRONZE");
            rows.add(row);
        }
        return rows;
    }

    @GET
    @Path("/loyalty")
    public List<Map<String, Object>> loyalty() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LoyaltyBean l : repository.findAllLoyalty()) {
            CustomerBean c = repository.findCustomerById(l.getCustomerId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerId", l.getCustomerId());
            row.put("name", c != null ? c.getName() : null);
            row.put("points", l.getPoints());
            row.put("tier", l.getTier());
            rows.add(row);
        }
        return rows;
    }

    @GET
    @Path("/history")
    public Response history(@QueryParam("customerId") String customerId) {
        if (customerId == null) {
            return Response.status(400).entity(Map.of("error", "customerId required")).build();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PurchaseHistoryBean ph : repository.findHistoryByCustomer(customerId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("historyId", ph.getHistoryId());
            row.put("customerId", ph.getCustomerId());
            row.put("saleId", ph.getSaleId());
            row.put("date", ph.getDate());
            row.put("amount", ph.getAmount());
            rows.add(row);
        }
        return Response.ok(rows).build();
    }

    @GET
    @Path("/points")
    public Response points(@QueryParam("customerId") String customerId) {
        if (customerId == null) {
            return Response.status(400).entity(Map.of("error", "customerId required")).build();
        }
        return Response.ok(Map.of(
                "customerId", customerId,
                "points", customer.getLoyaltyPoints(customerId),
                "tier", customer.getLoyaltyTier(customerId),
                "totalSpend", customer.getTotalSpend(customerId)
        )).build();
    }

    @POST
    @Path("/add")
    public Response add(Map<String, Object> body) {
        CustomerBean c = new CustomerBean();
        c.setCustomerId("CUST-" + System.currentTimeMillis());
        c.setName((String) body.get("name"));
        c.setEmail((String) body.getOrDefault("email", ""));
        c.setRegisteredStoreId((String) body.getOrDefault("registeredStoreId", "STORE_A"));
        repository.saveCustomer(c);
        return Response.ok(Map.of("customerId", c.getCustomerId(), "name", c.getName())).build();
    }

    @POST
    @Path("/points")
    public Response addPoints(Map<String, Object> body) {
        String customerId = (String) body.get("customerId");
        double amount = Double.parseDouble(body.get("saleAmount").toString());
        customer.addLoyaltyPoints(customerId, amount);
        return Response.ok(Map.of(
                "customerId", customerId,
                "points", customer.getLoyaltyPoints(customerId))).build();
    }

    /**
     * Redeems loyalty points for a discount: 100 points = 1 EGP.
     * Deducts the requested points (capped at available balance) and returns
     * the EGP discount value to apply to the sale.
     *
     * POST /api/customer/redeempoints
     * Body: { customerId, pointsToRedeem }
     */
    @POST
    @Path("/redeempoints")
    public Response redeemPoints(Map<String, Object> body) {
        String customerId = (String) body.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "customerId required")).build();
        }
        int requested = Integer.parseInt(body.getOrDefault("pointsToRedeem", "0").toString());
        int available = customer.getLoyaltyPoints(customerId);
        int toRedeem  = Math.min(requested, available);
        if (toRedeem <= 0) {
            return Response.status(400).entity(Map.of("error", "No points available to redeem")).build();
        }
        // Deduct points (negative delta)
        repository.addPoints(customerId, -toRedeem);
        double discountEgp = Math.round((toRedeem / 100.0) * 100.0) / 100.0;
        return Response.ok(Map.of(
                "customerId",   customerId,
                "pointsRedeemed", toRedeem,
                "discountEgp",  discountEgp,
                "remainingPoints", customer.getLoyaltyPoints(customerId)
        )).build();
    }
}
