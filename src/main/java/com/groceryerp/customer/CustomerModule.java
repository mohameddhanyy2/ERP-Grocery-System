package com.groceryerp.customer;

import com.groceryerp.interfaces.*;
import java.util.*;

/**
 * Customer Module — Component implementation.
 *
 * PROVIDED interfaces: ICustomerData, ILoyaltyService
 * REQUIRED interfaces: ISalesData (injected via IoC)
 */
public class CustomerModule implements ICustomerData, ILoyaltyService {

    // Required interface — injected via IoC
    private ISalesData salesData;

    public CustomerModule() {}

    public void setSalesData(ISalesData salesData) { this.salesData = salesData; }

    @Override
    public String getCustomerName(String customerId) {
        // TODO: Member 5 — lookup customer name
        return "Customer-" + customerId;
    }

    @Override
    public List<String> getPurchaseHistoryIds(String customerId) {
        // TODO: Member 5 — return purchase history IDs
        return new ArrayList<>();
    }

    @Override
    public int getLoyaltyPoints(String customerId) {
        // TODO: Member 5 — lookup loyalty points
        return 0;
    }

    @Override
    public void addLoyaltyPoints(String customerId, double saleAmount) {
        // TODO: Member 5 — add points based on saleAmount
    }

    @Override
    public String getLoyaltyTier(String customerId) {
        // TODO: Member 5 — BRONZE / SILVER / GOLD based on points
        return "BRONZE";
    }
}
