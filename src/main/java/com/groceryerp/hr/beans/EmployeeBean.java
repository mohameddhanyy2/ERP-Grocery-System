package com.groceryerp.hr.beans;
import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="employees")
/*
 * EmployeeBean — Entity Bean mapped to the {@code employees} table.
 * One row per employee. Bean type: @Entity.
 */
/** JavaBean representing an employee in the HR module. */
public class EmployeeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String storeId;
    private String name;
    private String role;
    private double hourlyRate;
    private String startDate;

    public EmployeeBean() {}

    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getStoreId() {
        return storeId;
    }
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }
    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getStartDate() {
        return startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the employees table. */
    public static class DAO {

        /** Inserts or replaces an EmployeeBean in the employees table. */
        public void save(EmployeeBean employee) {
            String sql = "INSERT OR REPLACE INTO employees (employeeId,storeId,name,role,hourlyRate,startDate) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, employee.getEmployeeId());
                ps.setString(2, employee.getStoreId());
                ps.setString(3, employee.getName());
                ps.setString(4, employee.getRole());
                ps.setDouble(5, employee.getHourlyRate());
                ps.setString(6, employee.getStartDate());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save employee: " + e.getMessage());
            }
        }

        /** Finds an EmployeeBean by ID, or returns null if not found. */
        public EmployeeBean findById(String employeeId) {
            String sql = "SELECT employeeId,storeId,name,role,hourlyRate,startDate FROM employees WHERE employeeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return mapRow(rs); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find employee: " + e.getMessage());
            }
            return null;
        }

        /** Returns all employee IDs for a given store. */
        public List<String> findIdsByStore(String storeId) {
            List<String> list = new ArrayList<>();
            String sql = "SELECT employeeId FROM employees WHERE storeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) { list.add(rs.getString("employeeId")); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to list employee IDs: " + e.getMessage());
            }
            return list;
        }

        /** Returns the number of employees in a given store. */
        public int countByStore(String storeId) {
            String sql = "SELECT COUNT(*) FROM employees WHERE storeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return rs.getInt(1); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to count employees: " + e.getMessage());
            }
            return 0;
        }

        private EmployeeBean mapRow(ResultSet rs) throws SQLException {
            EmployeeBean e = new EmployeeBean();
            e.setEmployeeId(rs.getString("employeeId"));
            e.setStoreId(rs.getString("storeId"));
            e.setName(rs.getString("name"));
            e.setRole(rs.getString("role"));
            e.setHourlyRate(rs.getDouble("hourlyRate"));
            e.setStartDate(rs.getString("startDate"));
            return e;
        }
    }
}


// conflicts resolved by: Omar Khalifa