package com.groceryerp.interfaces;

import jakarta.ejb.Local;
import java.util.List;
/** Provided by HRModule. Required by: FinanceModule, ReportingModule. */
@Local
public interface IStaffData {
    List<String> getStaffIdsByStore(String storeId);
    double getTotalPayrollCost(String period);
    int getStaffCount(String storeId);
}

// reviewed by: Omar Khalifa