package com.groceryerp.reporting.services;

import com.groceryerp.reporting.beans.ReportBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * ReportGenerator — @Stateless helper for the Reporting module.
 *
 * Populates ReportBean value objects from data fetched via {@link DataCollector}.
 * The collector was previously wired in through a setDataCollector() setter
 * (called from ReportingModule's constructor); it is now injected by the
 * container via {@code @EJB}, so the setter is gone.
 *
 * Each build method receives parameters, constructs a fresh ReportBean, and
 * returns it. Bean type: @Stateless — no conversational state.
 */
@Stateless
public class ReportGenerator {

    // ── Container-injected collaborator (replaces the setter-wired field) ──
    @EJB
    private DataCollector dataCollector;

    public ReportGenerator() { /* required no-arg constructor for the container */ }

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
