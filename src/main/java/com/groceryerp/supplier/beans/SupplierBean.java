package com.groceryerp.supplier.beans;
import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="suppliers")
/**
 * SupplierBean — Entity Bean mapped to the {@code suppliers} table.
 * One row per supplier. Bean type: @Entity.
 */
public class SupplierBean implements Serializable {

    // @Id
    /** Unique supplier identifier, e.g. "SUP_001". */
    private String supplierId;
    /** Supplier company name. */
    private String name;
    /** Contact email address. */
    private String contactEmail;
    /** Lead time in days for deliveries. */
    private int leadTimeDays;

    /** No-argument constructor required by JavaBeans spec. */
    public SupplierBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the suppliers table. */
    public static class DAO {

        /** Inserts or replaces a SupplierBean in the suppliers table. */
        public void save(SupplierBean supplier) {
            String sql = "INSERT OR REPLACE INTO suppliers (supplierId,name,contactEmail,leadTimeDays) VALUES (?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, supplier.getSupplierId());
                ps.setString(2, supplier.getName());
                ps.setString(3, supplier.getContactEmail());
                ps.setInt(4, supplier.getLeadTimeDays());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save supplier: " + e.getMessage());
            }
        }

        /** Finds a SupplierBean by ID, or returns null if not found. */
        public SupplierBean findById(String supplierId) {
            String sql = "SELECT supplierId,name,contactEmail,leadTimeDays FROM suppliers WHERE supplierId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, supplierId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        SupplierBean s = new SupplierBean();
                        s.setSupplierId(rs.getString("supplierId"));
                        s.setName(rs.getString("name"));
                        s.setContactEmail(rs.getString("contactEmail"));
                        s.setLeadTimeDays(rs.getInt("leadTimeDays"));
                        return s;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find supplier: " + e.getMessage());
            }
            return null;
        }

        /** Returns all supplier IDs in the suppliers table. */
        public List<String> findAllIds() {
            List<String> list = new ArrayList<>();
            String sql = "SELECT supplierId FROM suppliers";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { list.add(rs.getString("supplierId")); }
            } catch (SQLException e) {
                System.out.println("Failed to list supplier IDs: " + e.getMessage());
            }
            return list;
        }
    }
}

// conflicts resolved by: Omar Khalifa
