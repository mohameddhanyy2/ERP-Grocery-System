package com.groceryerp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/** Shared helpers for all API servlets. */
public abstract class BaseServlet extends HttpServlet {

    protected static final ObjectMapper JSON = new ObjectMapper();

    protected void json(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setStatus(HttpServletResponse.SC_OK);
        JSON.writeValue(resp.getWriter(), data);
    }

    protected void error(HttpServletResponse resp, int status, String msg) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setStatus(status);
        JSON.writeValue(resp.getWriter(), Map.of("error", msg));
    }

    protected String param(HttpServletRequest req, String name, String def) {
        String v = req.getParameter(name);
        return (v != null && !v.isBlank()) ? v : def;
    }

    /** Returns the last path segment after the servlet mapping. e.g. /api/hr/employees → "employees" */
    protected String pathAction(HttpServletRequest req) {
        String pi = req.getPathInfo();
        if (pi == null || pi.equals("/")) return "";
        return pi.replaceAll("^/+", "").split("/")[0];
    }
}
