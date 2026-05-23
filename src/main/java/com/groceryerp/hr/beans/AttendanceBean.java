package com.groceryerp.hr.beans;
import com.groceryerp.db.DatabaseManager;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// @Entity
// @Table(name="attendance")
/*
 * AttendanceBean — Entity Bean mapped to the {@code attendance} table.
 * One row per daily attendance record for an employee. Bean type: @Entity.
 */
/** JavaBean representing attendance in the HR module. */
public class AttendanceBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String attendanceId;
    private String date;
    private String employeeId;
    private String storeId;
    private boolean present;

    public AttendanceBean() {}

    public String getAttendanceId() {
        return attendanceId;
    }
    public void setAttendanceId(String attendanceId) {
        this.attendanceId = attendanceId;
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

    public String getStoreId() {
        return storeId;
    }
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public boolean isPresent() {
        return present;
    }
    public void setPresent(boolean present) {
        this.present = present;
    }

    // ── Nested DAO ─────────────────────────────────────────────────

    public static class DAO {

        public void save(AttendanceBean a) {
            String sql = "INSERT OR REPLACE INTO attendance (attendanceId,employeeId,storeId,date,present) VALUES (?,?,?,?,?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, a.getAttendanceId());
                ps.setString(2, a.getEmployeeId());
                ps.setString(3, a.getStoreId());
                ps.setString(4, a.getDate());
                ps.setInt(5, a.isPresent() ? 1 : 0);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Failed to save attendance: " + e.getMessage());
            }
        }
    }
}

// conflicts resolved by: Omar Khalifa