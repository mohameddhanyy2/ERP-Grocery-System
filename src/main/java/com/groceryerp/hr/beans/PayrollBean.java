package com.groceryerp.hr.beans;

import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// @Entity
// @Table(name="payroll")
/**
 * PayrollBean — Entity Bean mapped to the {@code payroll} table.
 * One row per payroll record per employee per period. Bean type: @Entity.
 */
public class PayrollBean implements Serializable {

    // @Id
    /** Unique payroll record identifier. */
    private String payrollId;
    /** ID of the employee this payroll is for. */
    private String employeeId;
    /** Pay period, e.g. "2026-05". */
    private String period;
    /** Gross pay before deductions. */
    private double grossPay;
    /** Total deductions amount. */
    private double deductions;
    /** Net pay after deductions. */
    private double netPay;

    /** No-argument constructor required by JavaBeans spec. */
    public PayrollBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getPayrollId() { return payrollId; }
    public void setPayrollId(String payrollId) { this.payrollId = payrollId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getGrossPay() { return grossPay; }
    public void setGrossPay(double grossPay) { this.grossPay = grossPay; }

    public double getDeductions() { return deductions; }
    public void setDeductions(double deductions) { this.deductions = deductions; }

    public double getNetPay() { return netPay; }
    public void setNetPay(double netPay) { this.netPay = netPay; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the payroll table. */
    public static class DAO {

        /** Persists a PayrollBean to the payroll table. */
        public void save(PayrollBean payroll) {
            String sql = "INSERT OR REPLACE INTO payroll (payrollId,employeeId,period,grossPay,deductions,netPay) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, payroll.getPayrollId());
                ps.setString(2, payroll.getEmployeeId());
                ps.setString(3, payroll.getPeriod());
                ps.setDouble(4, payroll.getGrossPay());
                ps.setDouble(5, payroll.getDeductions());
                ps.setDouble(6, payroll.getNetPay());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save payroll: " + e.getMessage());
            }
        }

        /** Finds a PayrollBean by employee ID and period, or returns null if not found. */
        public PayrollBean findByEmployeeAndPeriod(String employeeId, String period) {
            String sql = "SELECT payrollId,employeeId,period,grossPay,deductions,netPay FROM payroll WHERE employeeId = ? AND period = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, employeeId);
                ps.setString(2, period);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        PayrollBean p = new PayrollBean();
                        p.setPayrollId(rs.getString("payrollId"));
                        p.setEmployeeId(rs.getString("employeeId"));
                        p.setPeriod(rs.getString("period"));
                        p.setGrossPay(rs.getDouble("grossPay"));
                        p.setDeductions(rs.getDouble("deductions"));
                        p.setNetPay(rs.getDouble("netPay"));
                        return p;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find payroll: " + e.getMessage());
            }
            return null;
        }

        /** Returns the sum of netPay for all payroll records in a given period. */
        public double sumCostByPeriod(String period) {
            String sql = "SELECT SUM(netPay) FROM payroll WHERE period = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, period);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { return rs.getDouble(1); }
                }
            } catch (SQLException e) {
                System.out.println("Failed to sum payroll cost: " + e.getMessage());
            }
            return 0.0;
        }
    }
}
