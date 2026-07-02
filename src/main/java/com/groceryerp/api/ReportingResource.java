package com.groceryerp.api;

import com.groceryerp.reporting.ReportingModule;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ReportingResource — JAX-RS resource that replaces the old ReportingServlet.
 *
 * Both servlet endpoints are ported. The servlet held the ReportingModule via a
 * hand-built constructor (wired in ApiServer); here it is injected as an
 * {@code @EJB}. Query-param defaults preserved exactly from the old servlet
 * (report: storeId="", export: storeId="STORE_A"; both default period to the
 * current YearMonth).
 *
 *   GET /api/reporting/report?type=SALES&period=2026-05&storeId=STORE_A
 *   GET /api/reporting/export?type=SALES&period=2026-05&storeId=STORE_A&format=CSV
 */
@Path("/reporting")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportingResource {

    @EJB
    private ReportingModule reporting;

    @GET
    @Path("/report")
    public Response report(@QueryParam("type") @DefaultValue("SALES") String type,
                           @QueryParam("period") String period,
                           @QueryParam("storeId") @DefaultValue("") String storeId) {
        String resolvedPeriod = (period == null || period.isBlank())
                ? java.time.YearMonth.now().toString() : period;
        String report = reporting.generateReport(type, resolvedPeriod, storeId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("period", resolvedPeriod);
        body.put("storeId", storeId);
        body.put("report", report);
        return Response.ok(body).build();
    }

    @GET
    @Path("/export")
    public Response export(@QueryParam("type") @DefaultValue("SALES") String type,
                           @QueryParam("period") String period,
                           @QueryParam("storeId") @DefaultValue("STORE_A") String storeId) {
        String resolvedPeriod = (period == null || period.isBlank())
                ? java.time.YearMonth.now().toString() : period;
        String data = reporting.exportCSV(type, resolvedPeriod);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("period", resolvedPeriod);
        body.put("storeId", storeId);
        body.put("data", data);
        return Response.ok(body).build();
    }
}
