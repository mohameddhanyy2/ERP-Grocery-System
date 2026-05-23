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
 * ProductBean — Entity Bean mapped to the {@code products} table.
 * One row per product in the inventory catalogue. Bean type: @Entity.
 */
public class ProductBean implements Serializable {

    // @Id
    /** Unique product identifier, e.g. "PROD_001". */
    private String productId;
    /** Display name of the product. */
    private String name;
    /** Product category, e.g. "Dairy", "Bakery". */
    private String category;
    /** Unit price of the product. */
    private double price;
    /** Expiry date as a string (ISO-8601), or null if not applicable. */
    private String expiryDate;

    /** No-argument constructor required by JavaBeans spec. */
    public ProductBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getExpiryDate() { return expiryDate; }
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
