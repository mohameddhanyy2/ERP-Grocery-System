package com.groceryerp.interfaces;

import jakarta.ejb.Local;
/** Provided by HRModule. Calculates payroll per employee per period. */
@Local
public interface IPayrollService {
    double calculatePayroll(String employeeId, String period);
}

// reviewed by: Omar Khalifa
