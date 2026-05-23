package com.groceryerp.pos.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="payments")
/**
 * PaymentBean — Entity Bean mapped to the {@code payments} table.
 * One row per payment record for a sale. Bean type: @Entity.
 */
public class PaymentBean implements Serializable {

    // @Id
    /** Unique payment identifier, e.g. "PAY-1234567890". */
    private String paymentId;
    /** ID of the sale this payment is for. */
    private String saleId;
    /** Payment method: CASH, CARD, or WALLET. */
    private String method;
    /** Amount the customer paid. */
    private double amountPaid;
    /** Change returned to customer (amountPaid - grandTotal). */
    private double change;
    /** ISO-8601 timestamp of when the payment was processed. */
    private String processedAt;

    /** No-argument constructor required by JavaBeans spec. */
    public PaymentBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the payments table. */
    public static class DAO {

        /** Persists a PaymentBean to the payments table. */
        public void save(PaymentBean payment) {
            String sql = "INSERT OR REPLACE INTO payments (paymentId,saleId,method,amountPaid,change,processedAt) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, payment.getPaymentId());
                ps.setString(2, payment.getSaleId());
                ps.setString(3, payment.getMethod());
                ps.setDouble(4, payment.getAmountPaid());
                ps.setDouble(5, payment.getChange());
                ps.setString(6, payment.getProcessedAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save payment: " + e.getMessage());
            }
        }

        /** Finds a PaymentBean by sale ID, or returns null if not found. */
        public PaymentBean findBySaleId(String saleId) {
            String sql = "SELECT * FROM payments WHERE saleId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PaymentBean p = new PaymentBean();
                        p.setPaymentId(rs.getString("paymentId"));
                        p.setSaleId(rs.getString("saleId"));
                        p.setMethod(rs.getString("method"));
                        p.setAmountPaid(rs.getDouble("amountPaid"));
                        p.setChange(rs.getDouble("change"));
                        p.setProcessedAt(rs.getString("processedAt"));
                        return p;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find payment: " + e.getMessage());
            }
            return null;
        }
    }
}

// reviewed by: Omar Khalifa