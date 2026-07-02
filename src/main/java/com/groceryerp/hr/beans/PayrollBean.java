package com.groceryerp.hr.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * PayrollBean — JPA entity mapped to the {@code payroll} table.
 * One row per payroll record per employee per period.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT / SUM). The DAO is gone — persistence
 * is now handled by {@link com.groceryerp.hr.HRRepository} via the
 * container-managed EntityManager. The annotations below ARE the table mapping.
 */
@Entity
@Table(name = "payroll")
public class PayrollBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "payrollId")
    private String payrollId;

    @Column(name = "employeeId")
    private String employeeId;

    @Column(name = "period")
    private String period;

    @Column(name = "grossPay")
    private double grossPay;

    @Column(name = "deductions")
    private double deductions;

    @Column(name = "netPay")
    private double netPay;

    public PayrollBean() {}

    public String getPayrollId() {
        return payrollId;
    }
    public void setPayrollId(String payrollId) {
        this.payrollId = payrollId;
    }

    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPeriod() {
        return period;
    }
    public void setPeriod(String period) {
        this.period = period;
    }

    public double getGrossPay() {
        return grossPay;
    }
    public void setGrossPay(double grossPay) {
        this.grossPay = grossPay;
    }

    public double getDeductions() {
        return deductions;
    }
    public void setDeductions(double deductions) {
        this.deductions = deductions;
    }

    public double getNetPay() {
        return netPay;
    }
    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }
}
