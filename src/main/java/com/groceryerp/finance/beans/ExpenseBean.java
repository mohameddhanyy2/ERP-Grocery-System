package com.groceryerp.finance.beans;
import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

// @Entity
// @Table(name="expenses")
/**
 * ExpenseBean — Entity Bean mapped to the {@code expenses} table.
 * One row per expense record. Bean type: @Entity.
 */
/** JavaBean representing an Expense in the Finance module. */
public class ExpenseBean implements Serializable {
    private String expenseId;
    private String storeId;
    private String category;
    private double amount;
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

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the expenses table. */
    public static class DAO {

        /** Persists an ExpenseBean to the expenses table. */
        public void save(ExpenseBean expense) {
            String sql = "INSERT OR REPLACE INTO expenses (expenseId,storeId,category,amount,date) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, expense.getExpenseId());
                ps.setString(2, expense.getStoreId());
                ps.setString(3, expense.getCategory());
                ps.setDouble(4, expense.getAmount());
                ps.setString(5, expense.getDate());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save expense: " + e.getMessage());
            }
        }

        /** Returns the sum of amount for expenses whose date matches the given period prefix. */
        public double sumByPeriod(String period) {
            String sql = "SELECT SUM(amount) FROM expenses WHERE date LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, period + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return rs.getDouble(1); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to sum expenses: " + e.getMessage());
            }
            return 0.0;
        }
    }
}

// conflicts resolved by: Omar Khalifa