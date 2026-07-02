package com.groceryerp.interfaces;

import jakarta.ejb.Local;
/** Provided by ReportingModule. Consumer-only — no other module requires this. */
@Local
public interface IReportData {
    String generateReport(String reportType, String dateRange, String storeId);
    String exportCSV(String reportType, String dateRange);
}

// reviewed by: Omar Khalifa