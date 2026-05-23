package com.groceryerp.api;

import com.groceryerp.reporting.ReportingModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * GET /api/reporting/report?type=SALES&period=2026-05&storeId=STORE_A
 * GET /api/reporting/export?type=SALES&period=2026-05&storeId=STORE_A&format=CSV
 */
public class ReportingServlet extends BaseServlet {

    private final ReportingModule reporting;

    public ReportingServlet(ReportingModule reporting) { this.reporting = reporting; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (pathAction(req)) {
            case "report" -> handleReport(req, resp);
            case "export" -> handleExport(req, resp);
            default -> error(resp, 404, "Unknown reporting endpoint");
        }
    }

    private void handleReport(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type    = param(req, "type", "SALES");
        String period  = param(req, "period", java.time.YearMonth.now().toString());
        String storeId = param(req, "storeId", "");
        String report  = reporting.generateReport(type, period, storeId);
        json(resp, Map.of("type", type, "period", period, "storeId", storeId, "report", report));
    }

    private void handleExport(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type    = param(req, "type", "SALES");
        String period  = param(req, "period", java.time.YearMonth.now().toString());
        String storeId = param(req, "storeId", "STORE_A");
        String data    = reporting.exportCSV(type, period);
        json(resp, Map.of("type", type, "period", period, "storeId", storeId, "data", data));
    }
}
