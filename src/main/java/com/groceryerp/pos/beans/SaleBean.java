package com.groceryerp.pos.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="sales")
/**
 * SaleBean — Entity Bean mapped to the {@code sales} table.
 *
 * Represents one completed sale transaction row. Fields map directly to columns.
 * All persistence is handled by the nested DAO — no business logic in this class.
 *
 * Bean type: @Entity — one row in the sales table.
 */
public class SaleBean implements Serializable {

    // @Id
    /** Unique sale identifier, e.g. "SALE-1234567890". */
    private String saleId;
    /** ID of the store where the sale occurred. */
    private String storeId;
    /** ID of the customer who made the purchase. */
    private String customerId;
    /** Total monetary amount after discounts. */
    private double totalAmount;
    /** Payment method: CASH, CARD, or WALLET. */
    private String paymentMethod;
    /** ISO-8601 timestamp of when the sale was processed. */
    private String timestamp;
    /** Discount rate applied (0.0 to 1.0). */
    private double discountRate;
    /** Line items included in this sale. */
    private List<SaleItemBean> items;

    /** No-argument constructor required by JavaBeans spec. */
    public SaleBean() {
        this.items = new ArrayList<>();
    }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getDiscountRate() { return discountRate; }
    public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }

    public List<SaleItemBean> getItems() { return items; }
    public void setItems(List<SaleItemBean> items) { this.items = items; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the sales and sale_items tables. */
    public static class DAO {

        /** Persists a SaleBean to the sales table. */
        public void save(SaleBean sale) {
            String sql = "INSERT OR REPLACE INTO sales (saleId,storeId,customerId,totalAmount,paymentMethod,timestamp,discountRate) VALUES (?,?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sale.getSaleId());
                ps.setString(2, sale.getStoreId());
                ps.setString(3, sale.getCustomerId());
                ps.setDouble(4, sale.getTotalAmount());
                ps.setString(5, sale.getPaymentMethod());
                ps.setString(6, sale.getTimestamp());
                ps.setDouble(7, sale.getDiscountRate());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save sale: " + e.getMessage());
            }
        }

        /** Persists a SaleItemBean to the sale_items table. */
        public void saveItem(SaleItemBean item) {
            String sql = "INSERT OR REPLACE INTO sale_items (itemId,saleId,productId,quantity,unitPrice,lineTotal) VALUES (?,?,?,?,?,?)";
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
                System.out.println("Failed to save sale item: " + e.getMessage());
            }
        }

        /** Finds a SaleBean by its ID, or returns null if not found. */
        public SaleBean findById(String saleId) {
            String sql = "SELECT * FROM sales WHERE saleId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find sale: " + e.getMessage());
            }
            return null;
        }

        /** Returns all sales for a given store on a given date (date prefix match). */
        public List<SaleBean> findByStoreAndDate(String storeId, String date) {
            List<SaleBean> list = new ArrayList<>();
            String sql = "SELECT * FROM sales WHERE storeId = ? AND timestamp LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, storeId);
                ps.setString(2, date + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find sales: " + e.getMessage());
            }
            return list;
        }

        /** Returns the sum of totalAmount for a given store on a given date. Pass empty storeId for all stores. */
        public double sumRevenueByStoreAndDate(String storeId, String date) {
            boolean allStores = storeId == null || storeId.isEmpty();
            String sql = allStores
                ? "SELECT SUM(totalAmount) FROM sales WHERE timestamp LIKE ?"
                : "SELECT SUM(totalAmount) FROM sales WHERE storeId = ? AND timestamp LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (allStores) {
                    ps.setString(1, date + "%");
                } else {
                    ps.setString(1, storeId);
                    ps.setString(2, date + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to sum revenue: " + e.getMessage());
            }
            return 0.0;
        }

        /** Returns the sum of totalAmount for all sales by a given customer. */
        public double sumRevenueByCustomer(String customerId) {
            String sql = "SELECT SUM(totalAmount) FROM sales WHERE customerId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return rs.getDouble(1); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to sum revenue by customer: " + e.getMessage());
            }
            return 0.0;
        }

        /** Returns the count of transactions on a given date. */
        public int countByDate(String date) {
            String sql = "SELECT COUNT(*) FROM sales WHERE timestamp LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, date + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to count sales: " + e.getMessage());
            }
            return 0;
        }

        /** Returns all line items for a given sale ID. */
        public List<SaleItemBean> findItemsBySaleId(String saleId) {
            List<SaleItemBean> items = new ArrayList<>();
            String sql = "SELECT * FROM sale_items WHERE saleId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        SaleItemBean item = new SaleItemBean();
                        item.setItemId(rs.getString("itemId"));
                        item.setSaleId(rs.getString("saleId"));
                        item.setProductId(rs.getString("productId"));
                        item.setQuantity(rs.getInt("quantity"));
                        item.setUnitPrice(rs.getDouble("unitPrice"));
                        item.setLineTotal(rs.getDouble("lineTotal"));
                        items.add(item);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find sale items: " + e.getMessage());
            }
            return items;
        }

        private SaleBean mapRow(ResultSet rs) throws SQLException {
            SaleBean sale = new SaleBean();
            sale.setSaleId(rs.getString("saleId"));
            sale.setStoreId(rs.getString("storeId"));
            sale.setCustomerId(rs.getString("customerId"));
            sale.setTotalAmount(rs.getDouble("totalAmount"));
            sale.setPaymentMethod(rs.getString("paymentMethod"));
            sale.setTimestamp(rs.getString("timestamp"));
            sale.setDiscountRate(rs.getDouble("discountRate"));
            return sale;
        }
    }
}

// reviewed by: Omar Khalifa
