package com.groceryerp.customer.beans;

import java.io.Serializable;

/** JavaBean representing loyalty points for a customer. */
public class LoyaltyBean implements Serializable {
    private int customerId;
    private int points;
    private String tier;

    public LoyaltyBean() {}

    public LoyaltyBean(int customerId, int points, String tier) {
        this.customerId = customerId;
        this.points = points;
        this.tier = tier;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    @Override
    public String toString() {
        return "LoyaltyBean{" +
                "customerId=" + customerId +
                ", points=" + points +
                ", tier='" + tier + '\'' +
                '}';
    }
}
