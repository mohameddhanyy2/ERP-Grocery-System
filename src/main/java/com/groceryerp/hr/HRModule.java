package com.groceryerp.hr;

import com.groceryerp.hr.beans.EmployeeBean;
import com.groceryerp.hr.beans.PayrollBean;
import com.groceryerp.hr.beans.ShiftBean;
import com.groceryerp.interfaces.*;
import java.util.*;

/**
 * HR Module - Component implementation.
 *
 * PROVIDED interfaces: IStaffData, IPayrollService
 * REQUIRED interfaces: none (foundation module)
 *
 * AttendanceBean is included in the HR package as a future extension for
 * attendance tracking, but current payroll calculations use ShiftBean data.
 */
/*
 * HRModule simulates stateful behavior with internal lists for payroll
 * computation, shift aggregation, and cached payroll results per
 * employee-period. No EJB container is required.
 */
public class HRModule implements IStaffData, IPayrollService {

    private List<EmployeeBean> employees;
    private List<PayrollBean> payrolls;
    private List<ShiftBean> shifts;

    public HRModule() {
        employees = new ArrayList<>();
        payrolls = new ArrayList<>();
        shifts = new ArrayList<>();

        employees.add(new EmployeeBean("EMP001", "STORE001", "Ahmed Hassan", "Cashier", 45.0));
        employees.add(new EmployeeBean("EMP002", "STORE001", "Mona Ali", "Store Clerk", 40.0));
        employees.add(new EmployeeBean("EMP003", "STORE002", "Omar Samir", "Supervisor", 60.0));

        shifts.add(new ShiftBean("SHIFT001", "EMP001", "2026-05-01 09:00", "2026-05-01 17:00", 8.0));
        shifts.add(new ShiftBean("SHIFT002", "EMP001", "2026-05-02 09:00", "2026-05-02 17:00", 8.0));
        shifts.add(new ShiftBean("SHIFT003", "EMP002", "2026-05-01 10:00", "2026-05-01 18:00", 8.0));
        shifts.add(new ShiftBean("SHIFT004", "EMP003", "2026-05-01 08:00", "2026-05-01 16:00", 8.0));

        payrolls.add(new PayrollBean("PAY001", "EMP001", "2026-05", 720.0, 72.0, 648.0));
        payrolls.add(new PayrollBean("PAY002", "EMP002", "2026-05", 320.0, 32.0, 288.0));
    }

    @Override
    public List<String> getStaffIdsByStore(String storeId) {
        List<String> staffIds = new ArrayList<>();

        for (EmployeeBean employee : employees) {
            if (Objects.equals(employee.getStoreId(), storeId)) {
                staffIds.add(employee.getEmployeeId());
            }
        }

        return staffIds;
    }

    @Override
    public double getTotalPayrollCost(String period) {
        double total = 0.0;

        for (PayrollBean payroll : payrolls) {
            if (Objects.equals(payroll.getPeriod(), period)) {
                total += payroll.getNetPay();
            }
        }

        return total;
    }

    @Override
    public int getStaffCount(String storeId) {
        int count = 0;

        for (EmployeeBean employee : employees) {
            if (Objects.equals(employee.getStoreId(), storeId)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public double calculatePayroll(String employeeId, String period) {
        PayrollBean existingPayroll = findPayroll(employeeId, period);

        if (existingPayroll != null) {
            return existingPayroll.getNetPay();
        }

        EmployeeBean employee = findEmployee(employeeId);

        if (employee == null) {
            return 0.0;
        }

        double hoursWorked = getHoursWorked(employeeId, period);
        double grossPay = hoursWorked * employee.getHourlyRate();
        double deductions = grossPay * 0.10;
        double netPay = grossPay - deductions;

        String payrollId = "PAY" + (payrolls.size() + 1);
        PayrollBean payroll = new PayrollBean(payrollId, employeeId, period, grossPay, deductions, netPay);
        payrolls.add(payroll);

        return netPay;
    }

    private EmployeeBean findEmployee(String employeeId) {
        for (EmployeeBean employee : employees) {
            if (Objects.equals(employee.getEmployeeId(), employeeId)) {
                return employee;
            }
        }

        return null;
    }

    private PayrollBean findPayroll(String employeeId, String period) {
        for (PayrollBean payroll : payrolls) {
            if (Objects.equals(payroll.getEmployeeId(), employeeId) && Objects.equals(payroll.getPeriod(), period)) {
                return payroll;
            }
        }

        return null;
    }

    private double getHoursWorked(String employeeId, String period) {
        double totalHours = 0.0;

        for (ShiftBean shift : shifts) {
            if (Objects.equals(shift.getEmployeeId(), employeeId) && isShiftInPeriod(shift, period)) {
                totalHours += shift.getHoursWorked();
            }
        }

        return totalHours;
    }

    private boolean isShiftInPeriod(ShiftBean shift, String period) {
        // Simplified academic model: period is matched by String prefix, for example "2026-05".
        return period != null && shift.getShiftStart() != null && shift.getShiftStart().startsWith(period);
    }
}
