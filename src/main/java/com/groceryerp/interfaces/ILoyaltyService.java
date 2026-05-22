package com.groceryerp.interfaces;
/** Provided by CustomerModule. Manages loyalty points. */
public interface ILoyaltyService {
    int getLoyaltyPoints(String customerId);
    void addLoyaltyPoints(String customerId, double saleAmount);
    String getLoyaltyTier(String customerId);
}
