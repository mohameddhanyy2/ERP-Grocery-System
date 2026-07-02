package com.groceryerp.reporting.services;

import com.groceryerp.reporting.beans.ReportBean;
import jakarta.ejb.Stateless;

/**
 * ExportService — @Stateless helper for the Reporting module.
 *
 * Converts a ReportBean into a CSV string. Receives all data it needs through
 * the parameter — no instance state touched or modified.
 *
 * Bean type: @Stateless — pure transformation, no conversational state.
 */
@Stateless
public class ExportService {

    public ExportService() { /* required no-arg constructor for the container */ }

    /** Converts the given ReportBean to a single-row CSV string. */
    public String exportToCSV(ReportBean report) {
        StringBuilder sb = new StringBuilder();
        sb.append("reportType,dateRange,totalValue,recordCount,generatedAt,storeId\n");
        sb.append(report.getReportType()).append(",")
          .append(report.getDateRange()).append(",")
          .append(report.getTotalValue()).append(",")
          .append(report.getRecordCount()).append(",")
          .append(report.getGeneratedAt()).append(",")
          .append(report.getStoreId() != null ? report.getStoreId() : "ALL");
        return sb.toString();
    }
}
