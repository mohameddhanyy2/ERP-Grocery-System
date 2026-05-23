package com.groceryerp.hr.beans;

import java.io.Serializable;

/** JavaBean representing attendance in the HR module. */
public class AttendanceBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;
    private String employeeId;
    private boolean present;

    public AttendanceBean() {}

    public AttendanceBean(String date, String employeeId, boolean present) {
        this.date = date;
        this.employeeId = employeeId;
        this.present = present;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }
}
