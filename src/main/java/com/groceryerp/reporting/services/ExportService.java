package com.groceryerp.reporting.services;

import com.groceryerp.reporting.beans.ReportBean;

/**
 * ExportService — internal sub-component of the Reporting module.
 * Converts a ReportBean into a CSV string for download.
 */
public class ExportService {

    public ExportService() {}

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
