package com.groceryerp.pos.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="receipts")
/**
 * ReceiptBean — Entity Bean mapped to the {@code receipts} table.
 * One row per receipt issued after a completed sale. Bean type: @Entity.
 */
public class ReceiptBean implements Serializable {

    // @Id
    /** Unique receipt identifier, e.g. "RECEIPT-1234567890". */
    private String receiptId;
    /** ID of the sale this receipt belongs to. */
    private String saleId;
    /** ID of the store where the sale took place. */
    private String storeId;
    /** Total sale amount before tax. */
    private double totalAmount;
    /** Tax amount — 14% of totalAmount. */
    private double taxAmount;
    /** Grand total — totalAmount + taxAmount. */
    private double grandTotal;
    /** ISO-8601 timestamp of when the receipt was issued. */
    private String issuedAt;

    /** No-argument constructor required by JavaBeans spec. */
    public ReceiptBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the receipts table. */
    public static class DAO {

        /** Persists a ReceiptBean to the receipts table. */
        public void save(ReceiptBean receipt) {
            String sql = "INSERT OR REPLACE INTO receipts (receiptId,saleId,storeId,totalAmount,taxAmount,grandTotal,issuedAt) VALUES (?,?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, receipt.getReceiptId());
                ps.setString(2, receipt.getSaleId());
                ps.setString(3, receipt.getStoreId());
                ps.setDouble(4, receipt.getTotalAmount());
                ps.setDouble(5, receipt.getTaxAmount());
                ps.setDouble(6, receipt.getGrandTotal());
                ps.setString(7, receipt.getIssuedAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save receipt: " + e.getMessage());
            }
        }

        /** Finds a ReceiptBean by its sale ID, or returns null if not found. */
        public ReceiptBean findBySaleId(String saleId) {
            String sql = "SELECT * FROM receipts WHERE saleId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ReceiptBean r = new ReceiptBean();
                        r.setReceiptId(rs.getString("receiptId"));
                        r.setSaleId(rs.getString("saleId"));
                        r.setStoreId(rs.getString("storeId"));
                        r.setTotalAmount(rs.getDouble("totalAmount"));
                        r.setTaxAmount(rs.getDouble("taxAmount"));
                        r.setGrandTotal(rs.getDouble("grandTotal"));
                        r.setIssuedAt(rs.getString("issuedAt"));
                        return r;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find receipt: " + e.getMessage());
            }
            return null;
        }
    }
}

// reviewed by: Omar Khalifa
