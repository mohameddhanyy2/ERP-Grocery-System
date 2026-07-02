package com.groceryerp.finance.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * ExpenseBean — JPA entity mapped to the {@code expenses} table.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT SUM). The DAO is gone — persistence is
 * now handled by {@link com.groceryerp.finance.FinanceRepository} via the
 * container-managed EntityManager. The annotations below ARE the table mapping.
 */
@Entity
@Table(name = "expenses")
public class ExpenseBean implements Serializable {

    @Id
    @Column(name = "expenseId")
    private String expenseId;

    @Column(name = "storeId")
    private String storeId;

    @Column(name = "category")
    private String category;

    @Column(name = "amount")
    private double amount;

    @Column(name = "date")
    private String date;

    public ExpenseBean() {}

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}

// conflicts resolved by: Omar Khalifa
