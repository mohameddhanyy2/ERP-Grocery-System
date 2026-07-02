package com.groceryerp.api;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * ResetResource — wipes all transactional data so the demo can be restarted
 * without dropping the schema. Keeps structural rows (stores, suppliers,
 * products, supplier_products) intact; only clears sales, stock, orders,
 * deliveries, alerts, finance, HR, and customer transaction data.
 *
 * Called from Dashboard.jsx "Reset Database" button → POST /api/reset.
 */
@Path("/reset")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class ResetResource {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    @POST
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Response reset() {
        try {
            // Delete in dependency order (children before parents to avoid FK violations).

            // POS / Sales
            em.createQuery("DELETE FROM SaleItemBean").executeUpdate();
            em.createQuery("DELETE FROM ReceiptBean").executeUpdate();
            em.createQuery("DELETE FROM PaymentBean").executeUpdate();
            em.createQuery("DELETE FROM DiscountBean").executeUpdate();
            em.createQuery("DELETE FROM SaleBean").executeUpdate();

            // Supplier / Orders
            em.createQuery("DELETE FROM DeliveryBean").executeUpdate();
            em.createQuery("DELETE FROM OrderLineBean").executeUpdate();
            em.createQuery("DELETE FROM PurchaseOrderBean").executeUpdate();

            // Inventory
            em.createQuery("DELETE FROM StockAlertBean").executeUpdate();
            em.createQuery("DELETE FROM StockBean").executeUpdate();
            em.createQuery("DELETE FROM StoreBean").executeUpdate();
            em.createQuery("DELETE FROM ProductBean").executeUpdate();

            // Supplier catalogue
            em.createQuery("DELETE FROM SupplierProductBean").executeUpdate();
            em.createQuery("DELETE FROM SupplierBean").executeUpdate();

            // Finance
            em.createQuery("DELETE FROM ExpenseBean").executeUpdate();
            em.createQuery("DELETE FROM RevenueBean").executeUpdate();
            em.createQuery("DELETE FROM TaxBean").executeUpdate();

            // HR
            em.createQuery("DELETE FROM AttendanceBean").executeUpdate();
            em.createQuery("DELETE FROM PayrollBean").executeUpdate();
            em.createQuery("DELETE FROM ShiftBean").executeUpdate();

            // Customer transactions
            em.createQuery("DELETE FROM PurchaseHistoryBean").executeUpdate();
            em.createQuery("DELETE FROM LoyaltyBean").executeUpdate();

            return Response.ok(Map.of("status", "ok", "message", "All transaction data cleared.")).build();
        } catch (Exception e) {
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
