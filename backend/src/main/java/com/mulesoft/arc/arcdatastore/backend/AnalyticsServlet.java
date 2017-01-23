package com.mulesoft.arc.arcdatastore.backend;

import com.google.gson.Gson;
import com.mulesoft.arc.arcdatastore.backend.models.ErrorResponse;
import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet to manually create an API for ARC analytics.
 *
 * appsopt.com domain is blocked in China so the backed need custom domain.
 * So far Google failed to run GCE on custom domains.
 */
@SuppressWarnings("serial")
public class AnalyticsServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(AnalyticsServlet.class.getName());
    AnalyticsDatabase db;
    // Session timeout in milliseconds - 30 minutes.
    private static final int SESSION_TIMEOUT = 1800000; // 30 * 60 * 1000; - 30 minutes

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/query".equals(path)) {
            handleQuery(req, resp);
        } else if ("/random".equals(path)) {
            handleRandomData(req, resp);
        } else {
            reportError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + path);
            log.warning("Unknown path for doGET: " + path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/record".equals(path)) {
            handleRecord(req, resp);
        } else {
            reportError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + path);
            log.warning("Unknown path for doPost: " + path);
        }
    }

    /**
     * Prepare data for analysis.
     * This request require sd (start date) and ed (end date) parameters.
     * As a result it will return an JSON object with number of users and sessions.
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void handleQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sd = req.getParameter("sd");
        String ed = req.getParameter("ed");
        if (sd == null) {
            reportError(resp, 400, "'sd' (startDate) parameter is required");
            return;
        }
        if (ed == null) {
            reportError(resp, 400, "'ed' (endDate) parameter is required");
            return;
        }

        AnalyticsDatabase db = getDatabase(req);
        QueryResult result;
        try {
            result = db.queryAnalytics(sd, ed);
        } catch (Exception e) {
            reportError(resp, 400, e.getMessage());
            return;
        }
        Gson gson = new Gson();
        writeSuccess(resp, gson.toJson(result));
    }

    /**
     * Record a session.
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void handleRecord(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String appId = req.getParameter("ai");
        if (appId == null) {
            reportError(resp, 400, "'ai' (appId) parameter is missing but it's required");
            return;
        }
//        String t = req.getParameter("t");
//        if (t == null) {
//            reportError(resp, 400, "'t' (time) parameter is missing but it's required.");
//            return;
//        }

        String tz = req.getParameter("tz");
        if (tz == null) {
            reportError(resp, 400, "'tz' (timeZoneOffset) parameter is missing but it's required.");
            return;
        }


        Integer timeZoneOffset;
        try {
            timeZoneOffset = Integer.parseInt(tz);
        } catch (Exception e) {
            reportError(resp, 400, "'tz' (time) timeZoneOffset is invalid: " + tz);
            return;
        }

        AnalyticsDatabase db = getDatabase(req);
        InsertResult result;
        try {
            result = db.recordSession(appId, timeZoneOffset, null);
        } catch (Exception e) {
            reportError(resp, 400, e.getMessage());
            return;
        }
        Gson gson = new Gson();
        writeSuccess(resp, gson.toJson(result));
    }

    private void handleRandomData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        AnalyticsDatabase db = getDatabase(req);
//        db.generateRandomData();
        resp.setStatus(204);
    }

    private void reportError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        ErrorResponse r = new ErrorResponse();
        r.code = statusCode;
        r.message = message;

        Gson gson = new Gson();

        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.getWriter().print(gson.toJson(r));
    }

    private void writeSuccess(HttpServletResponse resp, String response) throws IOException {
        resp.setContentType("application/json");
        resp.getWriter().print(response);
    }

    private void dumpContext(HttpServletRequest req) {
        String pi = req.getPathInfo();
        String ru = req.getRequestURI();
        String cp = req.getContextPath();
        String sn = req.getServerName();
        System.out.println("Path info " + pi);
        System.out.println("Request URI " + ru);
        System.out.println("Context path " + cp);
        System.out.println("Server name " + sn);
    }

    private AnalyticsDatabase getDatabase(HttpServletRequest req) {
        if (req == null) {
            return new ObjectifyAnalytics();
        }
        String connection = req.getParameter("connection");
        if (connection != null && connection.equals("raw")) {
            return new DatastoreAnalyticsAccess();
        }
        return new ObjectifyAnalytics();
    }
}
