package com.groceryerp.pos.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

/**
 * DiscountBean — JPA entity mapped to the {@code discounts} table.
 * One row per discount applied to a sale.
 *
 * The nested DAO is gone — persistence is handled by
 * {@link com.groceryerp.pos.POSRepository}. Bean type: @Entity.
 */
@Entity
@Table(name = "discounts")
public class DiscountBean implements Serializable {

    @Id
    @Column(name = "discountId")
    /** Unique discount identifier, e.g. "DISC-1234567890". */
    private String discountId;
    /** ID of the sale this discount applies to. */
    @Column(name = "saleId")
    private String saleId;
    /** Discount type: PERCENTAGE or FIXED. */
    @Column(name = "discountType")
    private String discountType;
    /** Value of the discount (percentage 0-100 or fixed amount). */
    @Column(name = "discountValue")
    private double discountValue;
    /** Human-readable description of the discount. */
    @Column(name = "description")
    private String description;

    /** No-argument constructor */
    public DiscountBean() { }

    public String getDiscountId() { return discountId; }
    public void setDiscountId(String discountId) { this.discountId = discountId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

// reviewed by: Omar Khalifa
