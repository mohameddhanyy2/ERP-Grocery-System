package com.groceryerp.customer.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * LoyaltyBean — JPA entity mapped to the {@code loyalty} table.
 *
 * Was: a JavaBean with a nested DAO whose {@code addPoints()} used a SQLite
 * {@code ON CONFLICT ... CASE} upsert to both increment points AND recalculate
 * the tier in one statement. That database-specific SQL cannot be expressed in
 * portable JPA, so the increment + tier rules now live in Java inside
 * {@link com.groceryerp.customer.CustomerRepository#addPoints}.
 */
@Entity
@Table(name = "loyalty")
public class LoyaltyBean implements Serializable {

    @Id
    @Column(name = "customerId")
    private String customerId;

    @Column(name = "points")
    private int points;

    @Column(name = "tier")
    private String tier;

    public LoyaltyBean() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
}
