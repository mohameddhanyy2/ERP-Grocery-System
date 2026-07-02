package com.groceryerp.interfaces;

import jakarta.ejb.Local;
/** Provided by CustomerModule. Manages loyalty points. */
@Local
public interface ILoyaltyService {
    int getLoyaltyPoints(String customerId);
    void addLoyaltyPoints(String customerId, double saleAmount);
    String getLoyaltyTier(String customerId);
}

// reviewed by: Omar Khalifa