package com.groceryerp.customer;

// @Stateless
// Chosen because each method reads from or writes to the database via a DAO
// and returns immediately. No customer or loyalty state accumulates between calls.

import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.customer.beans.LoyaltyBean;
import com.groceryerp.customer.beans.PurchaseHistoryBean;
import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.ILoyaltyService;
import com.groceryerp.interfaces.ISalesData;

import java.util.List;

/**
 * CustomerModule — Stateless Session Bean for customer data and loyalty management.
 *
 * Every method reads from or writes to the database immediately and returns.
 * No customer or loyalty state is held in memory between calls.
 *
 * PROVIDED interfaces: ICustomerData, ILoyaltyService
 * REQUIRED interfaces: ISalesData (injected via IoC)
 *
 * Bean type: @Stateless — no conversational state, all data flows through parameters and DAOs.
 */
public class CustomerModule implements ICustomerData, ILoyaltyService {

    // ── Injected interface (infrastructure, not business state) ───
    private ISalesData salesData;

    // ── DAO fields (infrastructure, not business state) ───────────
    private final CustomerBean.DAO customerDao         = new CustomerBean.DAO();
    private final LoyaltyBean.DAO loyaltyDao           = new LoyaltyBean.DAO();
    private final PurchaseHistoryBean.DAO historyDao   = new PurchaseHistoryBean.DAO();

    public CustomerModule() { /* no-arg constructor required by IoC */ }

    /** Injects the sales data dependency. */
    public void setSalesData(ISalesData salesData) { this.salesData = salesData; }

    // ── ICustomerData (provided) ──────────────────────────────────

    /** Returns the customer's full name from the database, or a fallback if not found. */
    @Override
    public String getCustomerName(String customerId) {
        CustomerBean customer = customerDao.findById(customerId);
        if (customer == null) { return "Customer-" + customerId; }
        return customer.getName();
    }

    /** Returns all sale IDs from the purchase history for the given customer. */
    @Override
    public List<String> getPurchaseHistoryIds(String customerId) {
        return historyDao.findIdsByCustomer(customerId);
    }

    // ── ILoyaltyService (provided) ────────────────────────────────

    /** Returns the current loyalty points for the given customer, or 0 if no record exists. */
    @Override
    public int getLoyaltyPoints(String customerId) {
        LoyaltyBean loyalty = loyaltyDao.findByCustomerId(customerId);
        if (loyalty == null) { return 0; }
        return loyalty.getPoints();
    }

    /**
     * Adds loyalty points earned from a sale: points = (int)(saleAmount / 10).
     * Tier is recalculated automatically by LoyaltyBean.DAO.addPoints().
     */
    @Override
    public void addLoyaltyPoints(String customerId, double saleAmount) {
        int points = (int) (saleAmount / 10);
        loyaltyDao.addPoints(customerId, points);
    }

    /** Returns the loyalty tier (BRONZE / SILVER / GOLD) for the given customer. */
    @Override
    public String getLoyaltyTier(String customerId) {
        LoyaltyBean loyalty = loyaltyDao.findByCustomerId(customerId);
        if (loyalty == null) { return "BRONZE"; }
        return loyalty.getTier();
    }
}
