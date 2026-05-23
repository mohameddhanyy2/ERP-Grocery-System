package com.groceryerp.hr.beans;

import java.io.Serializable;

/** JavaBean representing a shift in the HR module. */
public class ShiftBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shiftId;
    private String employeeId;
    private String shiftStart;
    private String shiftEnd;
    private double hoursWorked;

    public ShiftBean() {}

    public ShiftBean(String shiftId, String employeeId, String shiftStart, String shiftEnd, double hoursWorked) {
        this.shiftId = shiftId;
        this.employeeId = employeeId;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.hoursWorked = hoursWorked;
    }

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
