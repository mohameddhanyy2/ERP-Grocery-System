package com.groceryerp.customer;

import com.groceryerp.customer.beans.CustomerBean;
import com.groceryerp.customer.beans.LoyaltyBean;
import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.ILoyaltyService;
import com.groceryerp.interfaces.ISalesData;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;

/**
 * CustomerModule — now a real @Stateless session bean (previously a plain object
 * manually instantiated and wired in Main.java).
 *
 * PROVIDED interfaces: ICustomerData, ILoyaltyService
 * REQUIRED interface:   ISalesData — injected by the container via @Inject
 *                       instead of the old setSalesData() setter call in Main.
 *
 * Persistence is delegated to the injected @Stateless {@link CustomerRepository}
 * (the old CustomerBean.DAO / LoyaltyBean.DAO / PurchaseHistoryBean.DAO fields).
 */
@Stateless
@LocalBean
public class CustomerModule implements ICustomerData, ILoyaltyService {

    /** Container-injected persistence service (replaces the 3 nested DAO fields). */
    @EJB
    private CustomerRepository repository;

    /**
     * Required cross-module dependency. Injected by CDI rather than wired by hand
     * in Main.java. POSModule is the producer of ISalesData.
     */
    @Inject
    private ISalesData salesData;

    public CustomerModule() { /* required no-arg constructor for the container */ }

    // ── ICustomerData (provided) ──────────────────────────────────

    @Override
    public String getCustomerName(String customerId) {
        CustomerBean customer = repository.findCustomerById(customerId);
        if (customer == null) { return "Customer-" + customerId + " not found"; }
        return customer.getName();
    }

    @Override
    public List<String> getPurchaseHistoryIds(String customerId) {
        return repository.findSaleIdsByCustomer(customerId);
    }

    // ── ILoyaltyService (provided) ────────────────────────────────

    @Override
    public int getLoyaltyPoints(String customerId) {
        LoyaltyBean loyalty = repository.findLoyaltyByCustomerId(customerId);
        if (loyalty == null) { return 0; }
        return loyalty.getPoints();
    }

    @Override
    public void addLoyaltyPoints(String customerId, double saleAmount) {
        int points = (int) (saleAmount / 10);
        repository.addPoints(customerId, points);
    }

    @Override
    public String getLoyaltyTier(String customerId) {
        LoyaltyBean loyalty = repository.findLoyaltyByCustomerId(customerId);
        if (loyalty == null) { return "BRONZE"; }
        return loyalty.getTier();
    }

    @Override
    public double getTotalSpend(String customerId) {
        return salesData.getTotalSpendByCustomer(customerId);
    }
}
