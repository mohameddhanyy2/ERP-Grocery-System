package com.groceryerp.hr.beans;
import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// @Entity
// @Table(name="shifts")
/*
 * ShiftBean — Entity Bean mapped to the {@code shifts} table.
 * One row per work shift for an employee. Bean type: @Entity.
 */

/* JavaBean representing a shift in the HR module. */
public class ShiftBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shiftId;
    private String employeeId;
    private String storeId;
    private String shiftStart;
    private String shiftEnd;
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

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the shifts table. */
    public static class DAO {

        /** Persists a ShiftBean to the shifts table. */
        public void save(ShiftBean shift) {
            String sql = "INSERT OR REPLACE INTO shifts (shiftId,employeeId,storeId,shiftStart,shiftEnd,hoursWorked) VALUES (?,?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, shift.getShiftId());
                ps.setString(2, shift.getEmployeeId());
                ps.setString(3, shift.getStoreId());
                ps.setString(4, shift.getShiftStart());
                ps.setString(5, shift.getShiftEnd());
                ps.setDouble(6, shift.getHoursWorked());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save shift: " + e.getMessage());
            }
        }

        /** Returns all shifts for an employee in a given period (shiftStart LIKE period%). */
        public List<ShiftBean> findByEmployeeAndPeriod(String employeeId, String period) {
            List<ShiftBean> list = new ArrayList<>();
            String sql = "SELECT shiftId,employeeId,storeId,shiftStart,shiftEnd,hoursWorked FROM shifts WHERE employeeId = ? AND shiftStart LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, employeeId);
                ps.setString(2, period + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ShiftBean s = new ShiftBean();
                        s.setShiftId(rs.getString("shiftId"));
                        s.setEmployeeId(rs.getString("employeeId"));
                        s.setStoreId(rs.getString("storeId"));
                        s.setShiftStart(rs.getString("shiftStart"));
                        s.setShiftEnd(rs.getString("shiftEnd"));
                        s.setHoursWorked(rs.getDouble("hoursWorked"));
                        list.add(s);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find shifts: " + e.getMessage());
            }
            return list;
        }
    }
}

// conflicts resolved by: Omar Khalifa