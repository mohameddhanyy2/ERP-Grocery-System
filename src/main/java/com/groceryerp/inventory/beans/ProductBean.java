package com.groceryerp.inventory.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="products")
/**
 * JavaBean representing a single product carried by the grocery chain.
 * <p>
 * Follows the JavaBeans specification: public class, public no-argument
 * constructor, all fields private, and a public getter/setter for every
 * field. Implements {@link Serializable} so it can be passed between
 * components and packaged units.
 */
public class ProductBean implements Serializable {

    /** Unique product code, e.g. "PROD_001". */
    private String productId;
    /** Human-readable product name, e.g. "Whole Milk 1L". */
    private String name;
    /** Product category, e.g. "Dairy". */
    private String category;
    /** Shelf price in the store currency. */
    private double price;
    /** Expiry date as a plain string, e.g. "2026-08-01". */
    private String expiryDate;

    /** Public no-argument constructor required by the JavaBeans spec. */
    public ProductBean() {}
    
    /** @return the unique product code. */
    public String getProductId() { return productId; }

    /** @param productId the unique product code to set. */
    public void setProductId(String productId) { this.productId = productId; }

    /** @return the human-readable product name. */
    public String getName() { return name; }

    /** @param name the product name to set. */
    public void setName(String name) { this.name = name; }

    /** @return the product category. */
    public String getCategory() { return category; }

    /** @param category the product category to set. */
    public void setCategory(String category) { this.category = category; }

    /** @return the shelf price. */
    public double getPrice() { return price; }

    /** @param price the shelf price to set. */
    public void setPrice(double price) { this.price = price; }

    /** @return the expiry date string. */
    public String getExpiryDate() { return expiryDate; }

    /** @param expiryDate the expiry date string to set. */
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the products table. */
    public static class DAO {

        /** Inserts or replaces a ProductBean in the products table. */
        public void save(ProductBean product) {
            String sql = "INSERT OR REPLACE INTO products (productId,name,category,price,expiryDate) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, product.getProductId());
                ps.setString(2, product.getName());
                ps.setString(3, product.getCategory());
                ps.setDouble(4, product.getPrice());
                ps.setString(5, product.getExpiryDate());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save product: " + e.getMessage());
            }
        }

        /** Finds a ProductBean by ID, or returns null if not found. */
        public ProductBean findById(String productId) {
            String sql = "SELECT productId,name,category,price,expiryDate FROM products WHERE productId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return mapRow(rs); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find product: " + e.getMessage());
            }
            return null;
        }

        /** Returns all products in the products table. */
        public List<ProductBean> findAll() {
            List<ProductBean> list = new ArrayList<>();
            String sql = "SELECT productId,name,category,price,expiryDate FROM products";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { list.add(mapRow(rs)); }
            } catch (SQLException e) {
                System.out.println("Failed to list products: " + e.getMessage());
            }
            return list;
        }

        private ProductBean mapRow(ResultSet rs) throws SQLException {
            ProductBean p = new ProductBean();
            p.setProductId(rs.getString("productId"));
            p.setName(rs.getString("name"));
            p.setCategory(rs.getString("category"));
            p.setPrice(rs.getDouble("price"));
            p.setExpiryDate(rs.getString("expiryDate"));
            return p;
        }
    }
}

// conflicts resolved by: Omar Khalifa