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

    /** Generates a detailed report for the given type, period, and store. */
    @Override
    public String generateReport(String reportType, String dateRange, String storeId) {
        String store = (storeId == null || storeId.isBlank()) ? "All Stores" : storeId;
        String generatedAt = java.time.LocalDateTime.now().toString().replace("T", " ").substring(0, 19);

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append(String.format("  %-36s%n", reportType + " REPORT"));
        sb.append("========================================\n");
        sb.append(String.format("  Period   : %s%n", dateRange));
        sb.append(String.format("  Store    : %s%n", store));
        sb.append(String.format("  Generated: %s%n", generatedAt));
        sb.append("----------------------------------------\n");

        if ("SALES".equals(reportType)) {
            double revenue     = dataCollector.fetchRevenue(dateRange);
            int    transactions = dataCollector.fetchTransactions(dateRange);
            double avgTicket   = transactions > 0 ? revenue / transactions : 0;
            double payroll     = dataCollector.fetchPayroll(dateRange);
            double expenses    = dataCollector.fetchExpenses(dateRange);
            double netProfit   = revenue - expenses - payroll;

            sb.append(String.format("  %-28s %10.2f EGP%n", "Total Revenue",       revenue));
            sb.append(String.format("  %-28s %10d%n",        "Transactions",        transactions));
            sb.append(String.format("  %-28s %10.2f EGP%n", "Avg Ticket Value",    avgTicket));
            sb.append("----------------------------------------\n");
            sb.append(String.format("  %-28s %10.2f EGP%n", "Total Expenses",      expenses));
            sb.append(String.format("  %-28s %10.2f EGP%n", "Total Payroll",       payroll));
            sb.append("----------------------------------------\n");
            sb.append(String.format("  %-28s %10.2f EGP%n", "Net Profit",          netProfit));

        } else if ("FINANCE".equals(reportType)) {
            double revenue   = dataCollector.fetchRevenue(dateRange);
            double expenses  = dataCollector.fetchExpenses(dateRange);
            double payroll   = dataCollector.fetchPayroll(dateRange);
            double netProfit = dataCollector.fetchNetProfit(dateRange);
            double margin    = revenue > 0 ? (netProfit / revenue) * 100 : 0;

            sb.append(String.format("  %-28s %10.2f EGP%n", "Gross Revenue",       revenue));
            sb.append("----------------------------------------\n");
            sb.append(String.format("  %-28s %10.2f EGP%n", "Operating Expenses",  expenses));
            sb.append(String.format("  %-28s %10.2f EGP%n", "Payroll Cost",        payroll));
            sb.append(String.format("  %-28s %10.2f EGP%n", "Total Costs",         expenses + payroll));
            sb.append("----------------------------------------\n");
            sb.append(String.format("  %-28s %10.2f EGP%n", "Net Profit",          netProfit));
            sb.append(String.format("  %-28s %9.1f%%%n",    "Profit Margin",       margin));

        } else if ("INVENTORY".equals(reportType)) {
            int totalStock = dataCollector.fetchTotalStockAllProducts();
            int lowCount   = dataCollector.fetchLowStockCount(storeId);

            sb.append(String.format("  %-28s %10d units%n", "Total Stock (all SKUs)", totalStock));
            sb.append(String.format("  %-28s %10d items%n", "Low-Stock Alerts",       lowCount));
        }

        sb.append("========================================\n");
        return sb.toString();
    }

    /** Exports a finance report for the given date range as a CSV string. */
    @Override
    public String exportCSV(String reportType, String dateRange) {
        ReportBean report = reportGenerator.buildFinanceReport(dateRange, null);
        return exportService.exportToCSV(report);
    }
}
