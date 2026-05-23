package com.groceryerp.finance.beans;
import java.io.Serializable;
import java.time.LocalDate;

/** JavaBean representing an Expense in the Finance module. */
public class ExpenseBean implements Serializable {
    public ExpenseBean() {}
    private String expenseId;
    private String storeId;
    private String category;
    private double amount;
    private LocalDate date;

    public ExpenseBean(String expenseId, String storeId, String category, double amount, LocalDate date) {
        this.expenseId = expenseId;
        this.storeId = storeId;
        this.category = category;
        this.amount = amount;
        this.date = date;
    }

    public String getExpenseId() { return expenseId; }
    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    @Override
    public String toString() {
        return "ExpenseBean{" +
                "expenseId='" + expenseId + '\'' +
                ", storeId='" + storeId + '\'' +
                ", category='" + category + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                '}';
    }
}
