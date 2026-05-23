package com.groceryerp.customer;

import com.groceryerp.interfaces.*;
import java.util.*;
import com.groceryerp.customer.beans.*;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Customer Module — Component implementation.
 *
 * PROVIDED interfaces: ICustomerData, ILoyaltyService
 * REQUIRED interfaces: ISalesData (injected via IoC)
 */
public class CustomerModule implements ICustomerData, ILoyaltyService {

    // Required interface — injected via IoC
    private ISalesData salesData;

    // In-memory stores
    private final Map<Integer, CustomerBean> customers = new ConcurrentHashMap<>();
    private final Map<Integer, LoyaltyBean> loyalties = new ConcurrentHashMap<>();
    private final Map<Integer, List<PurchaseHistoryBean>> histories = new ConcurrentHashMap<>();

    public CustomerModule() {}

    public void setSalesData(ISalesData salesData) { this.salesData = salesData; }

    @Override
    public String getCustomerName(String customerId) {
        if (customerId == null) return null;
        try {
            int id = Integer.parseInt(customerId);
            CustomerBean cb = customers.get(id);
            return cb != null ? cb.getName() : "Customer-" + customerId;
        } catch (NumberFormatException ex) {
            return "Customer-" + customerId;
        }
    }

    @Override
    public List<String> getPurchaseHistoryIds(String customerId) {
        if (customerId == null) return Collections.emptyList();
        try {
            int id = Integer.parseInt(customerId);
            List<PurchaseHistoryBean> list = histories.getOrDefault(id, Collections.emptyList());
            List<String> ids = new ArrayList<>();
            for (PurchaseHistoryBean p : list) ids.add(p.getSaleId());
            return ids;
        } catch (NumberFormatException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public int getLoyaltyPoints(String customerId) {
        if (customerId == null) return 0;
        try {
            int id = Integer.parseInt(customerId);
            LoyaltyBean lb = loyalties.get(id);
            return lb != null ? lb.getPoints() : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @Override
    public void addLoyaltyPoints(String customerId, double saleAmount) {
        if (customerId == null) return;
        try {
            int id = Integer.parseInt(customerId);
            int pointsToAdd = (int) Math.floor(saleAmount);
            LoyaltyBean lb = loyalties.computeIfAbsent(id, k -> new LoyaltyBean(id, 0, "Bronze"));
            lb.setPoints(lb.getPoints() + pointsToAdd);
            // update tier
            int pts = lb.getPoints();
            if (pts >= 1000) lb.setTier("Gold");
            else if (pts >= 500) lb.setTier("Silver");
            else lb.setTier("Bronze");
            loyalties.put(id, lb);
        } catch (NumberFormatException ex) {
            // ignore
        }
    }

    @Override
    public String getLoyaltyTier(String customerId) {
        if (customerId == null) return "Bronze";
        try {
            int id = Integer.parseInt(customerId);
            LoyaltyBean lb = loyalties.get(id);
            return lb != null ? lb.getTier() : "Bronze";
        } catch (NumberFormatException ex) {
            return "Bronze";
        }
    }

    // --- Additional API requested by task ---
    public CustomerBean getCustomerProfile(int customerId) {
        return customers.get(customerId);
    }

    public void addLoyaltyPoints(int customerId, double saleAmount) {
        addLoyaltyPoints(String.valueOf(customerId), saleAmount);
    }

    public List<PurchaseHistoryBean> getPurchaseHistory(int customerId) {
        return new ArrayList<>(histories.getOrDefault(customerId, Collections.emptyList()));
    }

    // helpers for demo usage
    public void addCustomer(CustomerBean customer) {
        if (customer == null) return;
        customers.put(customer.getCustomerId(), customer);
    }

    public void addPurchase(int customerId, PurchaseHistoryBean ph) {
        histories.computeIfAbsent(customerId, k -> new ArrayList<>()).add(ph);
    }
}
