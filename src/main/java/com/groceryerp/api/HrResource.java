package com.groceryerp.api;

import com.groceryerp.finance.FinanceRepository;
import com.groceryerp.finance.beans.ExpenseBean;
import com.groceryerp.hr.HRModule;
import com.groceryerp.hr.HRRepository;
import com.groceryerp.hr.beans.AttendanceBean;
import com.groceryerp.hr.beans.EmployeeBean;
import com.groceryerp.hr.beans.PayrollBean;
import com.groceryerp.hr.beans.ShiftBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HrResource — JAX-RS resource for all HR operations.
 *
 *   GET  /api/hr/employees                   — all employees (with pending salary + last paid)
 *   GET  /api/hr/employees?storeId=          — filter by store
 *   GET  /api/hr/attendance                  — all attendance records
 *   GET  /api/hr/attendance?employeeId=      — attendance for one employee (calendar view)
 *   GET  /api/hr/shifts                      — all shifts
 *   GET  /api/hr/payroll                     — all payroll records
 *   POST /api/hr/employee                    — add employee (name, phone, role, salary, shiftStart, shiftEnd, offDay)
 *   POST /api/hr/checkin                     — record attendance with automatic penalty calculation
 *   POST /api/hr/paysalary                   — pay pending salary → posts Finance expense, resets pendingSalary
 *   POST /api/hr/shift                       — add shift (legacy)
 *   POST /api/hr/runpayroll                  — run payroll (legacy)
 *   POST /api/hr/attendance                  — add attendance (legacy simple)
 */
@Path("/hr")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HrResource {

    @EJB
    private HRModule hr;

    @EJB
    private HRRepository repository;

    @EJB
    private FinanceRepository financeRepository;

    // ── GET employees ────────────────────────────────────────────────────────

    @GET
    @Path("/employees")
    public List<Map<String, Object>> employees(@QueryParam("storeId") String storeId) {
        List<EmployeeBean> employees = storeId != null
                ? repository.findEmployeesByStore(storeId)
                : repository.findAllEmployees();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EmployeeBean e : employees) {
            rows.add(employeeToMap(e));
        }
        return rows;
    }

    // ── GET attendance ───────────────────────────────────────────────────────

    @GET
    @Path("/attendance")
    public List<Map<String, Object>> attendance(@QueryParam("employeeId") String employeeId) {
        List<AttendanceBean> records = employeeId != null
                ? repository.findAttendanceByEmployee(employeeId)
                : repository.findAllAttendance();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AttendanceBean a : records) {
            EmployeeBean e = repository.findEmployeeById(a.getEmployeeId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attendanceId",  a.getAttendanceId());
            row.put("employeeId",    a.getEmployeeId());
            row.put("employeeName",  e != null ? e.getName() : null);
            row.put("storeId",       a.getStoreId());
            row.put("date",          a.getDate());
            row.put("present",       a.isPresent());
            row.put("checkInTime",   a.getCheckInTime());
            row.put("checkOutTime",  a.getCheckOutTime());
            row.put("minutesLate",   a.getMinutesLate());
            row.put("penaltyAmount", a.getPenaltyAmount());
            rows.add(row);
        }
        return rows;
    }

    // ── GET shifts ───────────────────────────────────────────────────────────

    @GET
    @Path("/shifts")
    public List<Map<String, Object>> shifts() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ShiftBean s : repository.findAllShifts()) {
            EmployeeBean e = repository.findEmployeeById(s.getEmployeeId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("shiftId",      s.getShiftId());
            row.put("employeeId",   s.getEmployeeId());
            row.put("employeeName", e != null ? e.getName() : null);
            row.put("storeId",      s.getStoreId());
            row.put("shiftStart",   s.getShiftStart());
            row.put("shiftEnd",     s.getShiftEnd());
            row.put("hoursWorked",  s.getHoursWorked());
            rows.add(row);
        }
        return rows;
    }

    // ── GET payroll ──────────────────────────────────────────────────────────

    @GET
    @Path("/payroll")
    public List<Map<String, Object>> payroll() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PayrollBean pr : repository.findAllPayroll()) {
            EmployeeBean e = repository.findEmployeeById(pr.getEmployeeId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("payrollId",    pr.getPayrollId());
            row.put("employeeId",   pr.getEmployeeId());
            row.put("employeeName", e != null ? e.getName() : null);
            row.put("period",       pr.getPeriod());
            row.put("grossPay",     pr.getGrossPay());
            row.put("deductions",   pr.getDeductions());
            row.put("netPay",       pr.getNetPay());
            rows.add(row);
        }
        return rows;
    }

    // ── POST /employee ───────────────────────────────────────────────────────

    @POST
    @Path("/employee")
    public Response addEmployee(Map<String, Object> body) {
        EmployeeBean e = new EmployeeBean();
        e.setEmployeeId("EMP-" + System.currentTimeMillis());
        e.setStoreId((String) body.get("storeId"));
        e.setName((String) body.get("name"));
        e.setPhone((String) body.getOrDefault("phone", ""));
        e.setRole((String) body.getOrDefault("role", "Staff"));
        e.setSalary(Double.parseDouble(body.getOrDefault("salary", "0").toString()));
        e.setHourlyRate(0);
        e.setShiftStart((String) body.getOrDefault("shiftStart", "09:00"));
        e.setShiftEnd((String) body.getOrDefault("shiftEnd", "17:00"));
        e.setOffDay((String) body.getOrDefault("offDay", "Friday"));
        e.setStartDate((String) body.getOrDefault("startDate", LocalDate.now().toString()));
        e.setPendingSalary(0.0);
        e.setLastPaidDate(null);
        repository.saveEmployee(e);
        return Response.ok(employeeToMap(e)).build();
    }

    // ── POST /checkin — core attendance logic ────────────────────────────────

    /**
     * Records employee attendance for today.
     * - Prevents duplicate check-in on the same calendar day.
     * - Compares actual check-in time against scheduled shiftStart.
     * - If late > 30 min: deducts one day's salary proportional to minutes late / total shift minutes.
     * - Auto-sets checkOut to the employee's scheduled shiftEnd.
     * - Adds the day's earned pay (daily rate − penalty) to employee.pendingSalary.
     */
    @POST
    @Path("/checkin")
    public Response checkIn(Map<String, Object> body) {
        String employeeId = (String) body.get("employeeId");
        if (employeeId == null || employeeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "employeeId required")).build();
        }

        EmployeeBean emp = repository.findEmployeeById(employeeId);
        if (emp == null) {
            return Response.status(404).entity(Map.of("error", "Employee not found")).build();
        }

        // Optional date override for backdated inserts (seeding/testing). Defaults to today.
        String today = body.containsKey("date") ? (String) body.get("date") : LocalDate.now().toString();
        if (repository.attendanceExistsForDate(employeeId, today)) {
            return Response.status(409).entity(Map.of("error", "Already checked in on " + today)).build();
        }

        // Actual check-in time (caller can pass "HH:mm", default to now)
        String checkInTimeStr = (String) body.getOrDefault("checkInTime",
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        String scheduledStart = emp.getShiftStart() != null ? emp.getShiftStart() : "09:00";
        String scheduledEnd   = emp.getShiftEnd()   != null ? emp.getShiftEnd()   : "17:00";

        // Parse times
        LocalTime actualIn    = LocalTime.parse(checkInTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime scheduled   = LocalTime.parse(scheduledStart, DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime schedEnd    = LocalTime.parse(scheduledEnd,   DateTimeFormatter.ofPattern("HH:mm"));

        long minutesLate = ChronoUnit.MINUTES.between(scheduled, actualIn);
        if (minutesLate < 0) minutesLate = 0; // arrived early

        // Daily rate = monthly salary / 26 working days
        double dailyRate  = emp.getSalary() / 26.0;
        double penalty    = 0.0;

        if (minutesLate > 30) {
            long totalShiftMinutes = ChronoUnit.MINUTES.between(scheduled, schedEnd);
            if (totalShiftMinutes > 0) {
                // Penalty proportional to minutes late relative to shift length
                penalty = Math.round((dailyRate * minutesLate / totalShiftMinutes) * 100.0) / 100.0;
            }
        }

        double earnedToday = Math.round((dailyRate - penalty) * 100.0) / 100.0;

        // Save attendance record
        AttendanceBean att = new AttendanceBean();
        att.setAttendanceId("ATT-" + System.currentTimeMillis());
        att.setEmployeeId(employeeId);
        att.setStoreId(emp.getStoreId());
        att.setDate(today);
        att.setPresent(true);
        att.setCheckInTime(checkInTimeStr);
        att.setCheckOutTime(scheduledEnd);
        att.setMinutesLate((int) minutesLate);
        att.setPenaltyAmount(penalty);
        repository.saveAttendance(att);

        // Accumulate into pendingSalary
        emp.setPendingSalary(Math.round((emp.getPendingSalary() + earnedToday) * 100.0) / 100.0);
        repository.saveEmployee(emp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attendanceId",  att.getAttendanceId());
        result.put("date",          today);
        result.put("checkInTime",   checkInTimeStr);
        result.put("checkOutTime",  scheduledEnd);
        result.put("minutesLate",   minutesLate);
        result.put("penaltyAmount", penalty);
        result.put("earnedToday",   earnedToday);
        result.put("pendingSalary", emp.getPendingSalary());
        return Response.ok(result).build();
    }

    // ── POST /paysalary ──────────────────────────────────────────────────────

    /**
     * Pays the employee's accumulated pendingSalary:
     * 1. Posts an expense to Finance (category = "SALARY").
     * 2. Resets employee.pendingSalary to 0.
     * 3. Sets employee.lastPaidDate to today.
     */
    @POST
    @Path("/paysalary")
    public Response paySalary(Map<String, Object> body) {
        String employeeId = (String) body.get("employeeId");
        if (employeeId == null || employeeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "employeeId required")).build();
        }

        EmployeeBean emp = repository.findEmployeeById(employeeId);
        if (emp == null) {
            return Response.status(404).entity(Map.of("error", "Employee not found")).build();
        }

        double amount = emp.getPendingSalary();
        if (amount <= 0) {
            return Response.status(400).entity(Map.of("error", "No pending salary to pay")).build();
        }

        String today  = LocalDate.now().toString();
        String period = today.substring(0, 7); // YYYY-MM

        // Post to Finance as an expense
        ExpenseBean exp = new ExpenseBean();
        exp.setExpenseId("EXP-SAL-" + employeeId + "-" + System.currentTimeMillis());
        exp.setStoreId(emp.getStoreId());
        exp.setCategory("SALARY");
        exp.setAmount(amount);
        exp.setDate(today);
        financeRepository.saveExpense(exp);

        // Reset pending salary and record payment date
        emp.setPendingSalary(0.0);
        emp.setLastPaidDate(today);
        repository.saveEmployee(emp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employeeId",   employeeId);
        result.put("amountPaid",   amount);
        result.put("lastPaidDate", today);
        result.put("period",       period);
        result.put("expenseId",    exp.getExpenseId());
        return Response.ok(result).build();
    }

    // ── POST /shift (legacy) ─────────────────────────────────────────────────

    @POST
    @Path("/shift")
    public Response addShift(Map<String, Object> body) {
        ShiftBean s = new ShiftBean();
        s.setShiftId("SHIFT-" + System.currentTimeMillis());
        s.setEmployeeId((String) body.get("employeeId"));
        s.setStoreId((String) body.get("storeId"));
        s.setShiftStart((String) body.get("shiftStart"));
        s.setShiftEnd((String) body.get("shiftEnd"));
        Object hw = body.get("hoursWorked");
        s.setHoursWorked(hw != null ? Double.parseDouble(hw.toString()) : 8.0);
        repository.saveShift(s);
        return Response.ok(Map.of("shiftId", s.getShiftId())).build();
    }

    // ── POST /runpayroll (legacy) ────────────────────────────────────────────

    @POST
    @Path("/runpayroll")
    public Response runPayroll(Map<String, Object> body) {
        String employeeId = (String) body.get("employeeId");
        String period     = (String) body.get("period");
        double netPay     = hr.calculatePayroll(employeeId, period);
        return Response.ok(Map.of(
                "employeeId", employeeId, "period", period, "netPay", netPay)).build();
    }

    // ── POST /attendance (legacy simple) ────────────────────────────────────

    @POST
    @Path("/attendance")
    public Response addAttendance(Map<String, Object> body) {
        String employeeId = (String) body.get("employeeId");
        if (employeeId == null || employeeId.isBlank()) {
            return Response.status(400).entity(Map.of("error", "employeeId required")).build();
        }
        EmployeeBean employee = repository.findEmployeeById(employeeId);
        if (employee == null) {
            return Response.status(404).entity(Map.of("error", "Employee not found: " + employeeId)).build();
        }
        AttendanceBean a = new AttendanceBean();
        a.setAttendanceId("ATT-" + System.currentTimeMillis());
        a.setEmployeeId(employeeId);
        a.setStoreId(employee.getStoreId());
        a.setDate(java.time.LocalDate.now().toString());
        a.setPresent(true);
        a.setCheckInTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        a.setCheckOutTime(employee.getShiftEnd() != null ? employee.getShiftEnd() : "17:00");
        repository.saveAttendance(a);
        return Response.ok(Map.of("attendanceId", a.getAttendanceId())).build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> employeeToMap(EmployeeBean e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("employeeId",    e.getEmployeeId());
        row.put("storeId",       e.getStoreId());
        row.put("name",          e.getName());
        row.put("phone",         e.getPhone());
        row.put("role",          e.getRole());
        row.put("salary",        e.getSalary());
        row.put("shiftStart",    e.getShiftStart());
        row.put("shiftEnd",      e.getShiftEnd());
        row.put("offDay",        e.getOffDay());
        row.put("startDate",     e.getStartDate());
        row.put("pendingSalary", e.getPendingSalary());
        row.put("lastPaidDate",  e.getLastPaidDate());
        return row;
    }
}
