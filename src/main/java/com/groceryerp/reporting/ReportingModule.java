package com.groceryerp.reporting;

import com.groceryerp.interfaces.*;
import com.groceryerp.reporting.beans.ReportBean;
import com.groceryerp.reporting.services.*;

/**
 * Reporting Module — Component implementation.
 *
 * PROVIDED interfaces: IReportData
 * REQUIRED interfaces: ISalesData, IStaffData, ITotalStock, IFinanceData, ICustomerData
 *
 * Consumer-only component: reads from all modules, writes to none.
 * All five required interfaces injected via IoC into DataCollector.
 */
public class ReportingModule implements IReportData {

    private DataCollector dataCollector;
    private ReportGenerator reportGenerator;
    private ExportService exportService;

    public ReportingModule() {
        this.dataCollector   = new DataCollector();
        this.reportGenerator = new ReportGenerator();
        this.exportService   = new ExportService();
        this.reportGenerator.setDataCollector(dataCollector);
    }

    // IoC setters — wire required interfaces into DataCollector
    public void setSalesData(ISalesData s)    { dataCollector.setSalesData(s); }
    public void setStaffData(IStaffData s)    { dataCollector.setStaffData(s); }
    public void setTotalStock(ITotalStock t)  { dataCollector.setTotalStock(t); }
    public void setFinanceData(IFinanceData f){ dataCollector.setFinanceData(f); }
    public void setCustomerData(ICustomerData c){ dataCollector.setCustomerData(c); }

    @Override
    public String generateReport(String reportType, String dateRange, String storeId) {
        ReportBean report;
        if ("SALES".equals(reportType)) {
            report = reportGenerator.buildSalesReport(dateRange, storeId);
        } else {
            report = reportGenerator.buildFinanceReport(dateRange, storeId);
        }
        return report.getReportType() + " | " + report.getTotalValue() + " | " + report.getGeneratedAt();
    }

    @Override
    public String exportCSV(String reportType, String dateRange) {
        ReportBean report = reportGenerator.buildFinanceReport(dateRange, null);
        return exportService.exportToCSV(report);
    }
}
