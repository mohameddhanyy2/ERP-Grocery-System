package com.groceryerp.hr.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "employees")
public class EmployeeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "employeeId")
    private String employeeId;

    @Column(name = "storeId")
    private String storeId;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "role")
    private String role;

    /** Monthly fixed salary in EGP */
    @Column(name = "salary")
    private double salary;

    /** Scheduled shift start time, e.g. "09:00" */
    @Column(name = "shiftStart")
    private String shiftStart;

    /** Scheduled shift end time, e.g. "17:00" */
    @Column(name = "shiftEnd")
    private String shiftEnd;

    /** Weekly off day, e.g. "Friday" */
    @Column(name = "offDay")
    private String offDay;

    @Column(name = "startDate")
    private String startDate;

    /** Salary earned since last payment (days worked × daily rate − penalties). Updated on each attendance check-in. */
    @Column(name = "pendingSalary")
    private double pendingSalary;

    /** ISO date of last salary payment, e.g. "2026-06-30" */
    @Column(name = "lastPaidDate")
    private String lastPaidDate;

    /** Kept for legacy compatibility — was hourlyRate, now unused in pay calculations but kept so old rows don't break */
    @Column(name = "hourlyRate")
    private double hourlyRate;

    public EmployeeBean() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getShiftStart() { return shiftStart; }
    public void setShiftStart(String shiftStart) { this.shiftStart = shiftStart; }

    public String getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(String shiftEnd) { this.shiftEnd = shiftEnd; }

    public String getOffDay() { return offDay; }
    public void setOffDay(String offDay) { this.offDay = offDay; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public double getPendingSalary() { return pendingSalary; }
    public void setPendingSalary(double pendingSalary) { this.pendingSalary = pendingSalary; }

    public String getLastPaidDate() { return lastPaidDate; }
    public void setLastPaidDate(String lastPaidDate) { this.lastPaidDate = lastPaidDate; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
}
