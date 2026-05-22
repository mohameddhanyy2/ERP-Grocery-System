package com.groceryerp.interfaces;
/** Provided by ReportingModule. Consumer-only — no other module requires this. */
public interface IReportData {
    String generateReport(String reportType, String dateRange, String storeId);
    String exportCSV(String reportType, String dateRange);
}
