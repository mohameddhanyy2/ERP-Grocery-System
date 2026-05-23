package com.groceryerp.hr.beans;

import java.io.Serializable;

/** JavaBean representing an employee in the HR module. */
public class EmployeeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String storeId;
    private String name;
    private String role;
    private double hourlyRate;

    public EmployeeBean() {}

    public EmployeeBean(String employeeId, String storeId, String name, String role, double hourlyRate) {
        this.employeeId = employeeId;
        this.storeId = storeId;
        this.name = name;
        this.role = role;
        this.hourlyRate = hourlyRate;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
}
