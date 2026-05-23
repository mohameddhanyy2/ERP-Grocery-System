package com.groceryerp.stubs;

import com.groceryerp.interfaces.ISalesData;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple stub for ISalesData used in demos/tests.
 */
public class SalesDataStub implements ISalesData {
    private final Map<String, Double> revenueByStoreDate = new HashMap<>();

    public SalesDataStub() {
        // sample data: key = storeId|date
        revenueByStoreDate.put("ALL|ALL", 15000.0);
        revenueByStoreDate.put("store-1|2026-05-23", 8000.0);
        revenueByStoreDate.put("store-2|2026-05-23", 7000.0);
    }

    @Override
    public double getTotalRevenueBySale(String storeId, String date) {
        if (storeId == null) storeId = "ALL";
        if (date == null) date = "ALL";
        String key = storeId + "|" + date;
        if (revenueByStoreDate.containsKey(key)) return revenueByStoreDate.get(key);
        // fallback: if asking for ALL, return total
        return revenueByStoreDate.getOrDefault("ALL|ALL", 0.0);
    }

    @Override
    public int getTransactionCount(String date) {
        return 120;
    }

    @Override
    public double getTotalSpendByCustomer(String customerId) {
        return 0.0;
    }
}
