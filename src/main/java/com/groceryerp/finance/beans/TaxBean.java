package com.groceryerp.finance.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * TaxBean — JPA entity mapped to the {@code tax_records} table.
 * One row per tax record per period.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT by period). The DAO is gone —
 * persistence is now handled by {@link com.groceryerp.finance.FinanceRepository}
 * via the container-managed EntityManager.
 */
@Entity
@Table(name = "tax_records")
public class TaxBean implements Serializable {

    @Id
    @Column(name = "taxId")
    private String taxId;

    @Column(name = "period")
    private String period;

    @Column(name = "taxRate")
    private double taxRate;

    @Column(name = "taxableAmount")
    private double taxableAmount;

    @Column(name = "taxOwed")
    private double taxOwed;

    public TaxBean() {}

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public double getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(double taxableAmount) { this.taxableAmount = taxableAmount; }

    public double getTaxOwed() { return taxOwed; }
    public void setTaxOwed(double taxOwed) { this.taxOwed = taxOwed; }
}

// conflicts resolved by: Omar Khalifa
