package com.groceryerp.pos.beans;

import com.groceryerp.db.DatabaseManager;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="sale_items")
/**
 * SaleItemBean — Entity Bean mapped to the {@code sale_items} table.
 * One row per line item within a sale. Bean type: @Entity.
 */
public class SaleItemBean implements Serializable {
    // @Id
    private String itemId;
    private String saleId;
    private String productId;
    private int quantity;
    private double unitPrice;
    private double lineTotal;

    public SaleItemBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    
    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }

    public static class DAO {
        public void save(SaleItemBean item) {
            String sql = "INSERT OR REPLACE INTO sale_items (item_id, sale_id, product_id, quantity, unit_price, line_total) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, item.getItemId());
                ps.setString(2, item.getSaleId());
                ps.setString(3, item.getProductId());
                ps.setInt(4, item.getQuantity());
                ps.setDouble(5, item.getUnitPrice());
                ps.setDouble(6, item.getLineTotal());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("SaleItemBean.DAO.save failed", e);
            }
        }

        public List<SaleItemBean> findBySaleId(String saleId) {
            String sql = "SELECT item_id, sale_id, product_id, quantity, unit_price, line_total FROM sale_items WHERE sale_id = ?";
            List<SaleItemBean> items = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SaleItemBean item = new SaleItemBean();
                        item.setItemId(rs.getString("item_id"));
                        item.setSaleId(rs.getString("sale_id"));
                        item.setProductId(rs.getString("product_id"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setUnitPrice(rs.getDouble("unit_price"));
                        item.setLineTotal(rs.getDouble("line_total"));
                        items.add(item);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("SaleItemBean.DAO.findBySaleId failed", e);
            }
            return items;
        }

        public SaleItemBean findById(String itemId) {
            String sql = "SELECT item_id, sale_id, product_id, quantity, unit_price, line_total FROM sale_items WHERE item_id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        SaleItemBean item = new SaleItemBean();
                        item.setItemId(rs.getString("item_id"));
                        item.setSaleId(rs.getString("sale_id"));
                        item.setProductId(rs.getString("product_id"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setUnitPrice(rs.getDouble("unit_price"));
                        item.setLineTotal(rs.getDouble("line_total"));
                        return item;
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("SaleItemBean.DAO.findById failed", e);
            }
            return null;
        }
    }
}

// reviewed by: Omar Khalifa