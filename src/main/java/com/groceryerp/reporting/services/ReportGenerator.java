package com.groceryerp.reporting.services;

import com.groceryerp.reporting.beans.ReportBean;

/**
 * ReportGenerator — internal sub-component of the Reporting module.
 * Populates ReportBean objects from data fetched by DataCollector.
 */
public class ReportGenerator {

    private DataCollector dataCollector;

    public ReportGenerator() {}

    public void setDataCollector(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
    }

    public ReportBean buildFinanceReport(String dateRange, String storeId) {
        ReportBean report = new ReportBean();
        report.setReportType("FINANCE");
        report.setDateRange(dateRange);
        report.setTotalValue(dataCollector.fetchNetProfit(dateRange));
        report.setGeneratedAt(java.time.LocalDateTime.now().toString());
        report.setStoreId(storeId);
        return report;
    }

    public ReportBean buildSalesReport(String dateRange, String storeId) {
        ReportBean report = new ReportBean();
        report.setReportType("SALES");
        report.setDateRange(dateRange);
        report.setTotalValue(dataCollector.fetchRevenue(dateRange));
        report.setRecordCount(dataCollector.fetchTransactions(dateRange));
        report.setGeneratedAt(java.time.LocalDateTime.now().toString());
        report.setStoreId(storeId);
        return report;
    }
}
