package com.groceryerp.stubs;

import com.groceryerp.interfaces.IOrderStatus;
import java.util.Arrays;
import java.util.List;

/**
 * Simple stub for IOrderStatus used in demos/tests.
 */
public class OrderStatusStub implements IOrderStatus {

    @Override
    public String getOrderStatus(String orderId) {
        return "DELIVERED";
    }

    @Override
    public double getTotalPurchaseCost(String period) {
        // Fixed purchase cost for demo purposes
        return 3000.0;
    }

    @Override
    public List<String> getOrderIdsByStore(String storeId) {
        return Arrays.asList("o1", "o2");
    }
}
