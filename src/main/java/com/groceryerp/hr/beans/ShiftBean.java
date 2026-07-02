package com.groceryerp.hr.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * ShiftBean — JPA entity mapped to the {@code shifts} table.
 * One row per work shift for an employee.
 *
 * Was: a plain JavaBean with a nested {@code DAO} static class that hand-wrote
 * SQLite SQL (INSERT OR REPLACE / SELECT ... LIKE). The DAO is gone —
 * persistence is now handled by {@link com.groceryerp.hr.HRRepository} via the
 * container-managed EntityManager. The annotations below ARE the table mapping.
 */
@Entity
@Table(name = "shifts")
public class ShiftBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "shiftId")
    private String shiftId;

    @Column(name = "employeeId")
    private String employeeId;

    @Column(name = "storeId")
    private String storeId;

    @Column(name = "shiftStart")
    private String shiftStart;

    @Column(name = "shiftEnd")
    private String shiftEnd;

    @Column(name = "hoursWorked")
    private double hoursWorked;

    public ShiftBean() {}

    public String getShiftId() {
        return shiftId;
    }
    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
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

    public String getShiftStart() {
        return shiftStart;
    }
    public void setShiftStart(String shiftStart) {
        this.shiftStart = shiftStart;
    }

    public String getShiftEnd() {
        return shiftEnd;
    }
    public void setShiftEnd(String shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
}
