package com.groceryerp.api;

import com.groceryerp.hr.HRModule;
import com.groceryerp.hr.beans.AttendanceBean;
import com.groceryerp.hr.beans.EmployeeBean;
import com.groceryerp.hr.beans.ShiftBean;
import com.groceryerp.db.DatabaseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * GET  /api/hr/employees              — all employees
 * GET  /api/hr/employees?storeId=     — employees at a store
 * GET  /api/hr/shifts                 — all shifts
 * GET  /api/hr/payroll                — all payroll records
 * GET  /api/hr/attendance             — all attendance records
 * POST /api/hr/employee               — add an employee
 * POST /api/hr/shift                  — add a shift
 * POST /api/hr/payroll/run            — run payroll for employee + period
 */
public class HrServlet extends BaseServlet {

    private final HRModule hr;

    public HrServlet(HRModule hr) { this.hr = hr; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "employees"  -> handleEmployees(req, resp);
            case "shifts"     -> handleShifts(resp);
            case "payroll"    -> handlePayroll(resp);
            case "attendance" -> handleAttendance(resp);
            default -> error(resp, 404, "Unknown HR GET endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "employee"    -> handleAddEmployee(req, resp);
            case "shift"       -> handleAddShift(req, resp);
            case "runpayroll"  -> handleRunPayroll(req, resp);
            case "attendance"  -> handleAddAttendance(req, resp);
            default -> error(resp, 404, "Unknown HR POST endpoint");
        }
    }

    private void handleEmployees(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String storeId = req.getParameter("storeId");
        String sql = storeId != null
                ? "SELECT e.*, st.storeName FROM employees e LEFT JOIN stores st ON e.storeId=st.storeId WHERE e.storeId=? ORDER BY e.name"
                : "SELECT e.*, st.storeName FROM employees e LEFT JOIN stores st ON e.storeId=st.storeId ORDER BY e.storeId, e.name";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (storeId != null) ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("employeeId", rs.getString("employeeId"));
                    row.put("storeId", rs.getString("storeId"));
                    row.put("storeName", rs.getString("storeName"));
                    row.put("name", rs.getString("name"));
                    row.put("role", rs.getString("role"));
                    row.put("hourlyRate", rs.getDouble("hourlyRate"));
                    row.put("startDate", rs.getString("startDate"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleShifts(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT sh.*, e.name as employeeName FROM shifts sh LEFT JOIN employees e ON sh.employeeId=e.employeeId ORDER BY sh.shiftStart DESC LIMIT 200";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("shiftId", rs.getString("shiftId"));
                row.put("employeeId", rs.getString("employeeId"));
                row.put("employeeName", rs.getString("employeeName"));
                row.put("storeId", rs.getString("storeId"));
                row.put("shiftStart", rs.getString("shiftStart"));
                row.put("shiftEnd", rs.getString("shiftEnd"));
                row.put("hoursWorked", rs.getDouble("hoursWorked"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handlePayroll(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT pr.*, e.name as employeeName FROM payroll pr LEFT JOIN employees e ON pr.employeeId=e.employeeId ORDER BY pr.period DESC LIMIT 200";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("payrollId", rs.getString("payrollId"));
                row.put("employeeId", rs.getString("employeeId"));
                row.put("employeeName", rs.getString("employeeName"));
                row.put("period", rs.getString("period"));
                row.put("grossPay", rs.getDouble("grossPay"));
                row.put("deductions", rs.getDouble("deductions"));
                row.put("netPay", rs.getDouble("netPay"));
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleAttendance(HttpServletResponse resp) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT a.*, e.name as employeeName FROM attendance a LEFT JOIN employees e ON a.employeeId=e.employeeId ORDER BY a.date DESC LIMIT 200";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("attendanceId", rs.getString("attendanceId"));
                row.put("employeeId", rs.getString("employeeId"));
                row.put("employeeName", rs.getString("employeeName"));
                row.put("storeId", rs.getString("storeId"));
                row.put("date", rs.getString("date"));
                row.put("present", rs.getInt("present") == 1);
                rows.add(row);
            }
        } catch (SQLException e) { error(resp, 500, e.getMessage()); return; }
        json(resp, rows);
    }

    private void handleAddEmployee(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            EmployeeBean e = new EmployeeBean();
            e.setEmployeeId("EMP-" + System.currentTimeMillis());
            e.setStoreId((String) body.get("storeId"));
            e.setName((String) body.get("name"));
            e.setRole((String) body.getOrDefault("role", "Staff"));
            e.setHourlyRate(Double.parseDouble(body.getOrDefault("hourlyRate", "15").toString()));
            e.setStartDate((String) body.getOrDefault("startDate", java.time.LocalDate.now().toString()));
            new EmployeeBean.DAO().save(e);
            json(resp, Map.of("employeeId", e.getEmployeeId(), "name", e.getName()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleAddShift(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            ShiftBean s = new ShiftBean();
            s.setShiftId("SHIFT-" + System.currentTimeMillis());
            s.setEmployeeId((String) body.get("employeeId"));
            s.setStoreId((String) body.get("storeId"));
            s.setShiftStart((String) body.get("shiftStart"));
            s.setShiftEnd((String) body.get("shiftEnd"));
            Object hw = body.get("hoursWorked");
            s.setHoursWorked(hw != null ? Double.parseDouble(hw.toString()) : 8.0);
            new ShiftBean.DAO().save(s);
            json(resp, Map.of("shiftId", s.getShiftId()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleRunPayroll(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String employeeId = (String) body.get("employeeId");
            String period     = (String) body.get("period");
            double netPay     = hr.calculatePayroll(employeeId, period);
            json(resp, Map.of("employeeId", employeeId, "period", period, "netPay", netPay));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }

    private void handleAddAttendance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = JSON.readValue(req.getReader(), Map.class);
            String employeeId = (String) body.get("employeeId");
            if (employeeId == null || employeeId.isBlank()) { error(resp, 400, "employeeId required"); return; }

            // Look up the employee's storeId from the database
            String storeId = null;
            String empSql = "SELECT storeId FROM employees WHERE employeeId = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(empSql)) {
                ps.setString(1, employeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) storeId = rs.getString("storeId");
                }
            }
            if (storeId == null) { error(resp, 404, "Employee not found: " + employeeId); return; }

            AttendanceBean a = new AttendanceBean();
            a.setAttendanceId("ATT-" + System.currentTimeMillis());
            a.setEmployeeId(employeeId);
            a.setStoreId(storeId);
            a.setDate(java.time.LocalDateTime.now().toString());
            a.setPresent(true);
            new AttendanceBean.DAO().save(a);
            json(resp, Map.of("attendanceId", a.getAttendanceId()));
        } catch (Exception e) { error(resp, 400, e.getMessage()); }
    }
}
