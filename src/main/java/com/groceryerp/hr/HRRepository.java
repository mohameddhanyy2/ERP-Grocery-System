package com.groceryerp.hr;

import com.groceryerp.hr.beans.AttendanceBean;
import com.groceryerp.hr.beans.EmployeeBean;
import com.groceryerp.hr.beans.PayrollBean;
import com.groceryerp.hr.beans.ShiftBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * HRRepository — @Stateless session bean that owns all persistence for the HR
 * domain. Replaces the four nested DAO classes that used to live inside
 * EmployeeBean, ShiftBean, PayrollBean and AttendanceBean.
 *
 * The container injects a transaction-scoped {@link EntityManager} via
 * {@code @PersistenceContext}; each business method runs in a container-managed
 * JTA transaction (the EJB default is REQUIRED), so there is no manual
 * Connection/commit/close as there was with JDBC.
 */
@Stateless
public class HRRepository {

    @PersistenceContext(unitName = "groceryerp")
    private EntityManager em;

    // ── EmployeeBean.DAO replacement ──────────────────────────────

    /** Replaces EmployeeBean.DAO.save() — em.merge() emits the dialect-correct upsert. */
    // Original SQL: INSERT OR REPLACE INTO employees (employeeId,storeId,name,role,hourlyRate,startDate) VALUES (?,?,?,?,?,?)
    public void saveEmployee(EmployeeBean employee) {
        em.merge(employee);
    }

    /** Replaces EmployeeBean.DAO.findById() — em.find() replaces the manual SELECT + row-map. */
    // Original SQL: SELECT employeeId,storeId,name,role,hourlyRate,startDate FROM employees WHERE employeeId = ?
    public EmployeeBean findEmployeeById(String employeeId) {
        return em.find(EmployeeBean.class, employeeId);
    }

    /** Replaces EmployeeBean.DAO.findIdsByStore(). */
    // Original SQL: SELECT employeeId FROM employees WHERE storeId = ?
    public List<String> findEmployeeIdsByStore(String storeId) {
        return em.createQuery(
                "SELECT e.employeeId FROM EmployeeBean e WHERE e.storeId = :sid", String.class)
                .setParameter("sid", storeId)
                .getResultList();
    }

    /** Replaces EmployeeBean.DAO.countByStore(). */
    // Original SQL: SELECT COUNT(*) FROM employees WHERE storeId = ?
    public int countEmployeesByStore(String storeId) {
        Long count = em.createQuery(
                "SELECT COUNT(e) FROM EmployeeBean e WHERE e.storeId = :sid", Long.class)
                .setParameter("sid", storeId)
                .getSingleResult();
        return count != null ? count.intValue() : 0;
    }

    /** Backs HrResource employees endpoint (all employees ordered by store, then name). */
    // Original SQL: SELECT e.*, st.storeName FROM employees e LEFT JOIN stores st ON e.storeId=st.storeId ORDER BY e.storeId, e.name
    public List<EmployeeBean> findAllEmployees() {
        return em.createQuery(
                "SELECT e FROM EmployeeBean e ORDER BY e.storeId, e.name", EmployeeBean.class)
                .getResultList();
    }

    /** Backs HrResource employees?storeId= endpoint. */
    // Original SQL: SELECT e.*, st.storeName FROM employees e LEFT JOIN stores st ON e.storeId=st.storeId WHERE e.storeId=? ORDER BY e.name
    public List<EmployeeBean> findEmployeesByStore(String storeId) {
        return em.createQuery(
                "SELECT e FROM EmployeeBean e WHERE e.storeId = :sid ORDER BY e.name", EmployeeBean.class)
                .setParameter("sid", storeId)
                .getResultList();
    }

    // ── ShiftBean.DAO replacement ─────────────────────────────────

    /** Replaces ShiftBean.DAO.save(). */
    // Original SQL: INSERT OR REPLACE INTO shifts (shiftId,employeeId,storeId,shiftStart,shiftEnd,hoursWorked) VALUES (?,?,?,?,?,?)
    public void saveShift(ShiftBean shift) {
        em.merge(shift);
    }

    /**
     * Replaces ShiftBean.DAO.findByEmployeeAndPeriod(). The original used a
     * SQLite {@code shiftStart LIKE period%} filter; reproduced here with a JPQL
     * LIKE and an appended '%' wildcard.
     */
    // Original SQL: SELECT shiftId,employeeId,storeId,shiftStart,shiftEnd,hoursWorked FROM shifts WHERE employeeId = ? AND shiftStart LIKE ?  (param: period + "%")
    public List<ShiftBean> findShiftsByEmployeeAndPeriod(String employeeId, String period) {
        return em.createQuery(
                "SELECT s FROM ShiftBean s WHERE s.employeeId = :eid AND s.shiftStart LIKE :period",
                ShiftBean.class)
                .setParameter("eid", employeeId)
                .setParameter("period", period + "%")
                .getResultList();
    }

    /** Backs HrResource shifts endpoint (most recent 200 shifts). */
    // Original SQL: SELECT sh.*, e.name as employeeName FROM shifts sh LEFT JOIN employees e ON sh.employeeId=e.employeeId ORDER BY sh.shiftStart DESC LIMIT 200
    public List<ShiftBean> findAllShifts() {
        return em.createQuery(
                "SELECT s FROM ShiftBean s ORDER BY s.shiftStart DESC", ShiftBean.class)
                .setMaxResults(200)
                .getResultList();
    }

    // ── PayrollBean.DAO replacement ───────────────────────────────

    /** Replaces PayrollBean.DAO.save(). */
    // Original SQL: INSERT OR REPLACE INTO payroll (payrollId,employeeId,period,grossPay,deductions,netPay) VALUES (?,?,?,?,?,?)
    public void savePayroll(PayrollBean payroll) {
        em.merge(payroll);
    }

    /** Replaces PayrollBean.DAO.findByEmployeeAndPeriod(). */
    // Original SQL: SELECT payrollId,employeeId,period,grossPay,deductions,netPay FROM payroll WHERE employeeId = ? AND period = ?
    public PayrollBean findPayrollByEmployeeAndPeriod(String employeeId, String period) {
        List<PayrollBean> rows = em.createQuery(
                "SELECT p FROM PayrollBean p WHERE p.employeeId = :eid AND p.period = :period",
                PayrollBean.class)
                .setParameter("eid", employeeId)
                .setParameter("period", period)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Replaces PayrollBean.DAO.sumCostByPeriod(). JPQL SUM over an empty set
     * returns null, so coalesce to 0.0 to preserve the original behaviour.
     */
    // Original SQL: SELECT SUM(netPay) FROM payroll WHERE period = ?
    public double sumCostByPeriod(String period) {
        Double total = em.createQuery(
                "SELECT SUM(p.netPay) FROM PayrollBean p WHERE p.period = :period", Double.class)
                .setParameter("period", period)
                .getSingleResult();
        return total != null ? total : 0.0;
    }

    /** Backs HrResource payroll endpoint (most recent 200 payroll records). */
    // Original SQL: SELECT pr.*, e.name as employeeName FROM payroll pr LEFT JOIN employees e ON pr.employeeId=e.employeeId ORDER BY pr.period DESC LIMIT 200
    public List<PayrollBean> findAllPayroll() {
        return em.createQuery(
                "SELECT p FROM PayrollBean p ORDER BY p.period DESC", PayrollBean.class)
                .setMaxResults(200)
                .getResultList();
    }

    // ── AttendanceBean.DAO replacement ────────────────────────────

    /**
     * Replaces AttendanceBean.DAO.save(). The original stored the boolean as an
     * integer 1/0; the JPA boolean mapping handles that conversion now.
     */
    // Original SQL: INSERT OR REPLACE INTO attendance (attendanceId,employeeId,storeId,date,present) VALUES (?,?,?,?,?)  (present as 1/0)
    public void saveAttendance(AttendanceBean attendance) {
        em.merge(attendance);
    }

    /** Backs HrResource attendance endpoint (most recent 200 attendance records). */
    public List<AttendanceBean> findAllAttendance() {
        return em.createQuery(
                "SELECT a FROM AttendanceBean a ORDER BY a.date DESC", AttendanceBean.class)
                .setMaxResults(200)
                .getResultList();
    }

    /** All attendance records for one employee — used by the calendar view. */
    public List<AttendanceBean> findAttendanceByEmployee(String employeeId) {
        return em.createQuery(
                "SELECT a FROM AttendanceBean a WHERE a.employeeId = :eid ORDER BY a.date DESC",
                AttendanceBean.class)
                .setParameter("eid", employeeId)
                .getResultList();
    }

    /** Check whether an attendance record already exists for this employee on this date. */
    public boolean attendanceExistsForDate(String employeeId, String date) {
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM AttendanceBean a WHERE a.employeeId = :eid AND a.date = :date",
                Long.class)
                .setParameter("eid", employeeId)
                .setParameter("date", date)
                .getSingleResult();
        return count != null && count > 0;
    }
}
