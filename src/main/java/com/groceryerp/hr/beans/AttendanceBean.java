package com.groceryerp.hr.beans;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "attendance")
public class AttendanceBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "attendanceId")
    private String attendanceId;

    @Column(name = "date")
    private String date;

    @Column(name = "employeeId")
    private String employeeId;

    @Column(name = "storeId")
    private String storeId;

    @Column(name = "present")
    private boolean present;

    /** Actual time the employee checked in, e.g. "09:47" */
    @Column(name = "checkInTime")
    private String checkInTime;

    /** Actual time the employee checked out — auto-set to scheduled shiftEnd on check-in */
    @Column(name = "checkOutTime")
    private String checkOutTime;

    /** Salary deducted for this day due to late arrival (> 30 min grace) */
    @Column(name = "penaltyAmount")
    private double penaltyAmount;

    /** Minutes the employee was late (0 if on time or within grace period) */
    @Column(name = "minutesLate")
    private int minutesLate;

    public AttendanceBean() { /* required by JPA */ }

    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }

    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }

    public double getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(double penaltyAmount) { this.penaltyAmount = penaltyAmount; }

    public int getMinutesLate() { return minutesLate; }
    public void setMinutesLate(int minutesLate) { this.minutesLate = minutesLate; }
}
