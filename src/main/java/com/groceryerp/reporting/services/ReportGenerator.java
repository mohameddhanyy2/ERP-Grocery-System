package com.groceryerp.reporting.services;

// @Stateless
// Receives a DataCollector and parameters per call, builds and returns a ReportBean.
// No state is held between calls.

import com.groceryerp.reporting.beans.ReportBean;

/**
 * ReportGenerator — Stateless helper for the Reporting module.
 *
 * Populates ReportBean objects from data fetched via DataCollector.
 * Each build method receives parameters, constructs a fresh ReportBean, and returns it.
 *
 * Bean type: @Stateless — no conversational state, every report is built on demand.
 */
public class ReportGenerator {

    // ── Injected service (infrastructure, not business state) ─────
    private DataCollector dataCollector;

    public ReportGenerator() { /* no-arg constructor required by IoC */ }

    /** Injects the data collector dependency. */
    public void setDataCollector(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
    }

    /** Builds a FINANCE report for the given date range and optional store. */
    public ReportBean buildFinanceReport(String dateRange, String storeId) {
        ReportBean report = new ReportBean();
        report.setReportType("FINANCE");
        report.setDateRange(dateRange);
        report.setTotalValue(dataCollector.fetchNetProfit(dateRange));
        report.setGeneratedAt(java.time.LocalDateTime.now().toString());
        report.setStoreId(storeId);
        return report;
    }

    /** Builds a SALES report for the given date range and optional store. */
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

    public ReportBean buildInventoryReport(String dateRange, String storeId) {
        ReportBean report = new ReportBean();
        report.setReportType("INVENTORY");
        report.setDateRange(dateRange);
        report.setTotalValue(dataCollector.fetchStockLevel("PROD_001")); // sample product
        report.setGeneratedAt(java.time.LocalDateTime.now().toString());
        report.setStoreId(storeId);
        return report;
    }
}
