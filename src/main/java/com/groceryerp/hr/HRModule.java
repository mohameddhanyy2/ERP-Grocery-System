package com.groceryerp.hr;

import com.groceryerp.hr.beans.EmployeeBean;
import com.groceryerp.hr.beans.PayrollBean;
import com.groceryerp.hr.beans.ShiftBean;
import com.groceryerp.interfaces.*;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import java.util.*;

/**
 * HR Module — now a real @Stateful session bean (previously a plain object
 * manually instantiated and wired in Main.java).
 *
 * Payroll is a multi-step session: beginPayrollSession() → loadShifts() →
 * computeGrossPay() → finalizePayroll() → endSession(). Each step stores its
 * result in session fields for the next step to consume.
 *
 * Stateless reads (getStaffIdsByStore, getTotalPayrollCost, getStaffCount) do
 * not touch session fields — they go straight to the repository.
 *
 * Bean type: @Stateful — intermediate payroll values accumulate across method
 * calls and must be held in instance state until endSession() clears them.
 * Persistence is delegated to the injected @Stateless {@link HRRepository}
 * (the old EmployeeBean.DAO / ShiftBean.DAO / PayrollBean.DAO fields).
 */
@Stateful
@LocalBean
public class HRModule implements IStaffData, IPayrollService {

    // ── Container-injected persistence service (replaces the 3 nested DAO fields) ──
    @EJB
    private HRRepository repository;

    // ── Session state fields ──────────────────────────────────────
    private String activeEmployeeId;
    private EmployeeBean activeEmployee;
    private List<ShiftBean> activeShifts;
    private double accumulatedHours;
    private double computedGrossPay;

    public HRModule() { /* required no-arg constructor for the container */ }

    // ── Stateful session lifecycle ────────────────────────────────

    /** Step 1 — begin a payroll session for the given employee. */
    public void beginPayrollSession(String employeeId) {
        this.activeEmployeeId = employeeId;
        this.activeEmployee   = repository.findEmployeeById(employeeId);
        this.activeShifts     = new ArrayList<>();
        this.accumulatedHours = 0.0;
        this.computedGrossPay = 0.0;
    }

    /** Step 2 — load all shifts for the active employee in the given period. */
    public void loadShifts(String period) {
        this.activeShifts = repository.findShiftsByEmployeeAndPeriod(activeEmployeeId, period);
        this.accumulatedHours = 0.0;
        for (ShiftBean shift : activeShifts) {
            accumulatedHours += shift.getHoursWorked();
        }
    }

    /** Step 3 — compute gross pay from accumulated hours and the employee's hourly rate. */
    public void computeGrossPay() {
        if (activeEmployee == null) {
            computedGrossPay = 0.0;
            return;
        }
        computedGrossPay = accumulatedHours * activeEmployee.getHourlyRate();
    }

    /**
     * Step 4 — apply 15% deductions, persist the payroll record, and return it.
     * Always call endSession() after this to clear session state.
     */
    public PayrollBean finalizePayroll(String period) {
        double deductions = Math.round(computedGrossPay * 0.15 * 100.0) / 100.0;
        double netPay     = Math.round((computedGrossPay - deductions) * 100.0) / 100.0;

        PayrollBean payroll = new PayrollBean();
        payroll.setPayrollId("PAY-" + activeEmployeeId + "-" + period);
        payroll.setEmployeeId(activeEmployeeId);
        payroll.setPeriod(period);
        payroll.setGrossPay(Math.round(computedGrossPay * 100.0) / 100.0);
        payroll.setDeductions(deductions);
        payroll.setNetPay(netPay);
        repository.savePayroll(payroll);
        return payroll;
    }

    /** Clears all session state. Must be called after finalizePayroll(). */
    @Remove
    public void endSession() {
        activeEmployeeId  = null;
        activeEmployee    = null;
        activeShifts      = null;
        accumulatedHours  = 0.0;
        computedGrossPay  = 0.0;
    }

    // ── IPayrollService (provided) ────────────────────────────────

    @Override
    public double calculatePayroll(String employeeId, String period) {
        beginPayrollSession(employeeId);
        loadShifts(period);
        computeGrossPay();
        PayrollBean result = finalizePayroll(period);
        endSession();
        return result.getNetPay();
    }

    // ── IStaffData (provided) — stateless reads ───────────────────

    @Override
    public List<String> getStaffIdsByStore(String storeId) {
        return repository.findEmployeeIdsByStore(storeId);
    }

    @Override
    public double getTotalPayrollCost(String period) {
        return repository.sumCostByPeriod(period);
    }

    @Override
    public int getStaffCount(String storeId) {
        return repository.countEmployeesByStore(storeId);
    }
}
