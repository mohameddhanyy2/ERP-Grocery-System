package com.groceryerp.customer.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="customers")
/*
 * CustomerBean — Entity Bean mapped to the {@code customers} table.
 * One row per customer. Bean type: @Entity.
 */
/** JavaBean representing a Customer in the Customer module. */
public class CustomerBean implements Serializable {
    private String customerId;
    private String name;
    private String email;
    private String registeredStoreId;

    public CustomerBean() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRegisteredStoreId() { return registeredStoreId; }
    public void setRegisteredStoreId(String registeredStoreId) { this.registeredStoreId = registeredStoreId; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the customers table. */
    public static class DAO {

        /** Inserts or replaces a CustomerBean in the customers table. */
        public void save(CustomerBean customer) {
            String sql = "INSERT OR REPLACE INTO customers (customerId,name,email,registeredStoreId) VALUES (?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customer.getCustomerId());
                ps.setString(2, customer.getName());
                ps.setString(3, customer.getEmail());
                ps.setString(4, customer.getRegisteredStoreId());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save customer: " + e.getMessage());
            }
        }

        /** Finds a CustomerBean by ID, or returns null if not found. */
        public CustomerBean findById(String customerId) {
            String sql = "SELECT customerId,name,email,registeredStoreId FROM customers WHERE customerId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        CustomerBean c = new CustomerBean();
                        c.setCustomerId(rs.getString("customerId"));
                        c.setName(rs.getString("name"));
                        c.setEmail(rs.getString("email"));
                        c.setRegisteredStoreId(rs.getString("registeredStoreId"));
                        return c;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find customer: " + e.getMessage());
            }
            return null;
        }
    }
}

// reviewed by: Omar Khalifa
