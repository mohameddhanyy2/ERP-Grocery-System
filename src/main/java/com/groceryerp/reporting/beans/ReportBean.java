package com.groceryerp.reporting.beans;
import java.io.Serializable;

// @Stateless (value object — computed on demand, not stored in a table, no DAO)
/**
 * ReportBean — computed result object for the Reporting module.
 * Not persisted. Bean type: @Stateless value object.
 */
public class ReportBean implements Serializable {
    private String reportType;
    private String dateRange;
    private double totalValue;
    private int recordCount;
    private String generatedAt;
    private String storeId;   // null = chain-wide report

    public ReportBean() { /* no-arg constructor required by JavaBeans spec */ }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }
    public double getTotalValue() { return totalValue; }
    public void setTotalValue(double totalValue) { this.totalValue = totalValue; }
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
}
