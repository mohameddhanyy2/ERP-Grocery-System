package com.groceryerp.api;

import com.groceryerp.finance.FinanceModule;
import com.groceryerp.finance.FinanceRepository;
import com.groceryerp.finance.beans.ExpenseBean;
import com.groceryerp.finance.beans.RevenueBean;
import com.groceryerp.finance.beans.TaxBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FinanceResource — JAX-RS resource that replaces the old FinanceServlet.
 *
 * The original servlet held its OWN raw SQL (SELECT * FROM revenue / expenses /
 * tax_records), bypassing the module. Those queries are reproduced here via the
 * injected @Stateless {@link FinanceRepository} plus JPQL, so no SQL strings
 * remain in the web layer.
 *
 *   GET  /api/finance/summary?period=   — revenue, expenses, net profit
 *   GET  /api/finance/revenue           — all revenue rows
 *   GET  /api/finance/expenses          — all expense rows
 *   GET  /api/finance/tax               — all tax records
 *   POST /api/finance/expense           — add an expense
 *   POST /api/finance/revenue           — add a revenue entry
 */
@Path("/finance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FinanceResource {

    @EJB
    private FinanceModule finance;

    @EJB
    private FinanceRepository repository;

    @GET
    @Path("/summary")
    public Response summary(@QueryParam("period") String period) {
        if (period == null) {
            period = java.time.YearMonth.now().toString();
        }
        double revenue  = finance.getTotalRevenue(period);
        double expenses = finance.getTotalExpenses(period);
        double profit   = finance.getNetProfit(period);
        return Response.ok(Map.of(
                "period", period,
                "totalRevenue", revenue,
                "totalExpenses", expenses,
                "netProfit", profit
        )).build();
    }

    @GET
    @Path("/revenue")
    public List<Map<String, Object>> revenue() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RevenueBean r : repository.findAllRevenue()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("revenueId", r.getRevenueId());
            row.put("period", r.getPeriod());
            row.put("storeId", r.getStoreId());
            row.put("grossRevenue", r.getGrossRevenue());
            row.put("saleId", r.getSaleId());
            rows.add(row);
        }
        // Sort newest first by revenueId (REV-<timestamp>)
        rows.sort((a, b) -> String.valueOf(b.get("revenueId")).compareTo(String.valueOf(a.get("revenueId"))));
        return rows;
    }

    @GET
    @Path("/expenses")
    public List<Map<String, Object>> expenses() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExpenseBean e : repository.findAllExpenses()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("expenseId", e.getExpenseId());
            row.put("storeId", e.getStoreId());
            row.put("category", e.getCategory());
            row.put("amount", e.getAmount());
            row.put("date", e.getDate());
            rows.add(row);
        }
        // Sort newest first by expenseId (EXP-<timestamp>)
        rows.sort((a, b) -> String.valueOf(b.get("expenseId")).compareTo(String.valueOf(a.get("expenseId"))));
        return rows;
    }

    @GET
    @Path("/tax")
    public List<Map<String, Object>> tax() {
        // Old SQL: SELECT * FROM tax_records ORDER BY period DESC LIMIT 100
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TaxBean t : repository.findAllTax()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("taxId", t.getTaxId());
            row.put("period", t.getPeriod());
            row.put("taxRate", t.getTaxRate());
            row.put("taxableAmount", t.getTaxableAmount());
            row.put("taxOwed", t.getTaxOwed());
            rows.add(row);
        }
        return rows;
    }

    @POST
    @Path("/expense")
    public Response addExpense(Map<String, Object> body) {
        try {
            ExpenseBean e = new ExpenseBean();
            e.setExpenseId("EXP-" + System.currentTimeMillis());
            e.setStoreId((String) body.get("storeId"));
            e.setCategory((String) body.getOrDefault("category", "MISC"));
            e.setAmount(Double.parseDouble(body.get("amount").toString()));
            e.setDate((String) body.getOrDefault("date", java.time.LocalDate.now().toString()));
            repository.saveExpense(e);
            return Response.ok(Map.of("expenseId", e.getExpenseId(), "amount", e.getAmount())).build();
        } catch (Exception ex) {
            return Response.status(400).entity(Map.of("error", String.valueOf(ex.getMessage()))).build();
        }
    }

    @POST
    @Path("/revenue")
    public Response addRevenue(Map<String, Object> body) {
        try {
            RevenueBean r = new RevenueBean();
            r.setRevenueId("REV-" + System.currentTimeMillis());
            r.setPeriod((String) body.get("period"));
            r.setStoreId((String) body.get("storeId"));
            r.setGrossRevenue(Double.parseDouble(body.get("grossRevenue").toString()));
            repository.saveRevenue(r);
            return Response.ok(Map.of("revenueId", r.getRevenueId(), "grossRevenue", r.getGrossRevenue())).build();
        } catch (Exception ex) {
            return Response.status(400).entity(Map.of("error", String.valueOf(ex.getMessage()))).build();
        }
    }
}
