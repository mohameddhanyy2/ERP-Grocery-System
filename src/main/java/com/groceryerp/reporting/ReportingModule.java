package com.groceryerp.reporting;

import com.groceryerp.interfaces.IReportData;
import com.groceryerp.reporting.beans.ReportBean;
import com.groceryerp.reporting.services.DataCollector;
import com.groceryerp.reporting.services.ExportService;
import com.groceryerp.reporting.services.ReportGenerator;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * ReportingModule — @Stateless Session Bean. Consumer-only component.
 *
 * Reads from all modules via five required interfaces, writes to none. Each call
 * to generateReport() or exportCSV() produces a fresh result with no state
 * carried over from previous calls.
 *
 * PROVIDED interfaces: IReportData
 * REQUIRED interfaces: ISalesData, IStaffData, ITotalStock, IFinanceData, ICustomerData
 *
 * MIGRATION: the five setSalesData/setStaffData/setTotalStock/setFinanceData/
 * setCustomerData setters (formerly called from Main to forward dependencies into
 * the internal DataCollector) are GONE. The required interfaces are now injected
 * by the container directly into {@link DataCollector} via {@code @Inject}, so
 * this module no longer forwards them. The three service helpers, previously
 * {@code new}-ed and hand-wired in the constructor, are now container-managed
 * {@code @Stateless} beans injected via {@code @EJB}.
 *
 * Bean type: @Stateless — no conversational state, every report is computed on demand.
 */
@Stateless
@LocalBean
public class ReportingModule implements IReportData {

    // ── Container-injected service collaborators (replace the new-ed fields) ──
    @EJB
    private DataCollector dataCollector;
    @EJB
    private ReportGenerator reportGenerator;
    @EJB
    private ExportService exportService;

    public ReportingModule() { /* required no-arg constructor for the container */ }

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
