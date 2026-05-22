package com.groceryerp.pos.beans;
import java.io.Serializable;

/** JavaBean representing a discount applied to a sale. */
public class DiscountBean implements Serializable {
    private String discountId;
    private String saleId;
    private String discountType;  // PERCENTAGE, FIXED
    private double discountValue;
    private String description;

    public DiscountBean() {}

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
