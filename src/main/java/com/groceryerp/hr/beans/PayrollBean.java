package com.groceryerp.hr.beans;

import java.io.Serializable;

/** JavaBean representing payroll in the HR module. */
public class PayrollBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String payrollId;
    private String employeeId;
    private String period;
    private double grossPay;
    private double deductions;
    private double netPay;

    public PayrollBean() {}

    public PayrollBean(String payrollId, String employeeId, String period,
                       double grossPay, double deductions, double netPay) {
        this.payrollId = payrollId;
        this.employeeId = employeeId;
        this.period = period;
        this.grossPay = grossPay;
        this.deductions = deductions;
        this.netPay = netPay;
    }

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
