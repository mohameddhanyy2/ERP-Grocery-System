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
// @Table(name="attendance")
/**
 * AttendanceBean — Entity Bean mapped to the {@code attendance} table.
 * One row per daily attendance record for an employee. Bean type: @Entity.
 */
public class AttendanceBean implements Serializable {

    // @Id
    /** Unique attendance record identifier. */
    private String attendanceId;
    /** ID of the employee. */
    private String employeeId;
    /** ID of the store. */
    private String storeId;
    /** ISO-8601 date of the attendance record. */
    private String date;
    /** 1 if present, 0 if absent. */
    private int present;

    /** No-argument constructor required by JavaBeans spec. */
    public AttendanceBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getPresent() { return present; }
    public void setPresent(int present) { this.present = present; }

    // ── Nested DAO ─────────────────────────────────────────────────

    /** Handles all persistence operations for the attendance table. */
    public static class DAO {

        /** Persists an AttendanceBean to the attendance table. */
        public void save(AttendanceBean attendance) {
            String sql = "INSERT OR REPLACE INTO attendance (attendanceId,employeeId,storeId,date,present) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, attendance.getAttendanceId());
                ps.setString(2, attendance.getEmployeeId());
                ps.setString(3, attendance.getStoreId());
                ps.setString(4, attendance.getDate());
                ps.setInt(5, attendance.getPresent());
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save attendance: " + e.getMessage());
            }
        }

        /** Returns all attendance records for an employee in a given period (date LIKE period%). */
        public List<AttendanceBean> findByEmployeeAndPeriod(String employeeId, String period) {
            List<AttendanceBean> list = new ArrayList<>();
            String sql = "SELECT attendanceId,employeeId,storeId,date,present FROM attendance WHERE employeeId = ? AND date LIKE ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, employeeId);
                ps.setString(2, period + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AttendanceBean a = new AttendanceBean();
                        a.setAttendanceId(rs.getString("attendanceId"));
                        a.setEmployeeId(rs.getString("employeeId"));
                        a.setStoreId(rs.getString("storeId"));
                        a.setDate(rs.getString("date"));
                        a.setPresent(rs.getInt("present"));
                        list.add(a);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Failed to find attendance: " + e.getMessage());
            }
            return list;
        }
    }
}
