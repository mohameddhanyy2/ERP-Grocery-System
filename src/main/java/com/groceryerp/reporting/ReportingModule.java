package com.groceryerp.reporting;

// @Stateless
// Chosen because each report is generated fresh per call — no report state
// is carried between calls. The module holds only final service fields.

import com.groceryerp.interfaces.ICustomerData;
import com.groceryerp.interfaces.IFinanceData;
import com.groceryerp.interfaces.IReportData;
import com.groceryerp.interfaces.ISalesData;
import com.groceryerp.interfaces.IStaffData;
import com.groceryerp.interfaces.ITotalStock;
import com.groceryerp.reporting.beans.ReportBean;
import com.groceryerp.reporting.services.DataCollector;
import com.groceryerp.reporting.services.ExportService;
import com.groceryerp.reporting.services.ReportGenerator;

/**
 * ReportingModule — Stateless Session Bean. Consumer-only component.
 *
 * Reads from all modules via five injected interfaces, writes to none.
 * Each call to generateReport() or exportCSV() produces a fresh result
 * with no state carried over from previous calls.
 *
 * PROVIDED interfaces: IReportData
 * REQUIRED interfaces: ISalesData, IStaffData, ITotalStock, IFinanceData, ICustomerData
 *
 * Bean type: @Stateless — no conversational state, every report is computed on demand.
 */
public class ReportingModule implements IReportData {

    // ── Internal service fields (infrastructure, not business state) ──
    private final DataCollector dataCollector;
    private final ReportGenerator reportGenerator;
    private final ExportService exportService;

    public ReportingModule() {
        this.dataCollector   = new DataCollector();
        this.reportGenerator = new ReportGenerator();
        this.exportService   = new ExportService();
        this.reportGenerator.setDataCollector(dataCollector);
    }

    // ── IoC setters — wire required interfaces into DataCollector ──

    /** Injects the sales data dependency. */
    public void setSalesData(ISalesData s)      { dataCollector.setSalesData(s); }

    /** Injects the staff data dependency. */
    public void setStaffData(IStaffData s)      { dataCollector.setStaffData(s); }

    /** Injects the total stock dependency. */
    public void setTotalStock(ITotalStock t)    { dataCollector.setTotalStock(t); }

    /** Injects the finance data dependency. */
    public void setFinanceData(IFinanceData f)  { dataCollector.setFinanceData(f); }

    /** Injects the customer data dependency. */
    public void setCustomerData(ICustomerData c){ dataCollector.setCustomerData(c); }

    // ── IReportData (provided) ────────────────────────────────────

    /** Generates a SALES or FINANCE report for the given date range and store. */
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

    /** Exports a finance report for the given date range as a CSV string. */
    @Override
    public String exportCSV(String reportType, String dateRange) {
        ReportBean report = reportGenerator.buildFinanceReport(dateRange, null);
        return exportService.exportToCSV(report);
    }
}
