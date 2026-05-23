package com.groceryerp.finance.beans;

import java.io.Serializable;

/** JavaBean representing tax calculation details. */
public class TaxBean implements Serializable {
    private double taxRate;
    private double taxableAmount;
    private double taxOwed;

    public TaxBean() {}

    public TaxBean(double taxRate, double taxableAmount, double taxOwed) {
        this.taxRate = taxRate;
        this.taxableAmount = taxableAmount;
        this.taxOwed = taxOwed;
    }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public double getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(double taxableAmount) { this.taxableAmount = taxableAmount; }

    public double getTaxOwed() { return taxOwed; }
    public void setTaxOwed(double taxOwed) { this.taxOwed = taxOwed; }

    @Override
    public String toString() {
        return "TaxBean{" +
                "taxRate=" + taxRate +
                ", taxableAmount=" + taxableAmount +
                ", taxOwed=" + taxOwed +
                '}';
    }
}
