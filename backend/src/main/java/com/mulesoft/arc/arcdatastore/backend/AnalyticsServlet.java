package com.mulesoft.arc.arcdatastore.backend;

import com.google.gson.Gson;
import com.mulesoft.arc.arcdatastore.backend.models.ErrorResponse;
import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/query".equals(path)) {
            handleQuery(req, resp);
        } else if ("/random".equals(path)) {
            handleRandomData(req, resp);
        } else if ("/analyse-day".equals(path)) {
            handleAnalyseDay(req, resp);
        } else if ("/analyse-week".equals(path)) {
            handleAnalyseWeek(req, resp);
        } else if ("/analyse-month".equals(path)) {
            handleAnalyseMonth(req, resp);
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
     * @throws IOException
     */
    private void handleRecord(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String apiVersion = req.getHeader("x-api-version");
        if (apiVersion == null || "1".equals(apiVersion)) {
            handleRecordV1(req, resp);
        } else if ("2".equals(apiVersion)) {
            handleRecordV2(req, resp);
        } else {
            reportError(resp, 400, "Unsupported API version");
        }
    }

    private void handleRecordV1(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

    private void handleRecordV2(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] allowed = { "aid", "t", "tz" };

        String tz = null;
        String t = null;
        String anonymousId = null;


        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if (!isMultipart) {
            reportError(resp, 400, "Content type of the request is not allowed. Use multipart/form-data");
            return;
        }

        DiskFileItemFactory factory = new DiskFileItemFactory();
        File repository = new File("~/");
        factory.setRepository(repository);
        factory.setSizeThreshold(100 * 1024);
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            List<FileItem> items = upload.parseRequest(req);
            for (FileItem item: items) {
                if (item.isFormField()) {
                    String fieldName = item.getFieldName();
                    int index = java.util.Arrays.binarySearch(allowed, fieldName);
                    if (index < 0) {
                        continue;
                    }
                    if ("aid".equals(fieldName)) {
                        anonymousId = item.getString();
                    } else if ("t".equals(fieldName)) {
                        t = item.getString();
                    } else if ("tz".equals(fieldName)) {
                        tz = item.getString();
                    }
                }
            }
        } catch (FileUploadException e) {
            reportError(resp, 400, e.getMessage());
            return;
        }

        String message = "";
        if (anonymousId == null) {
            message = "The `aid` (anonymousId) parameter is required. ";
        }
        if (t == null) {
            message = message.concat("The `t` (time) parameter is required. ");
        }
        if (tz == null) {
            message = message.concat("The `tz` (timeZoneOffset) parameter is required. ");
        }

        if (!message.equals("")) {
            reportError(resp, 400, message);
            return;
        }

        Long time = null;
        Integer timeZoneOffset = null;
        try {
            timeZoneOffset = Integer.parseInt(tz);
        } catch (Exception e) {
            message = "'tz' (timeZoneOffset) is invalid: " + tz + ". Expecting integer.";
        }
        try {
            time = Long.parseLong(t);
        } catch (Exception e) {
            message = "'t' (time) is invalid: " + t + ". Expecting long.";
        }

        if (!message.equals("")) {
            reportError(resp, 400, message);
            return;
        }

        AnalyticsDatabase db = new DatastoreAnalyticsAccess();
        InsertResult result;
        try {
            result = db.recordSession(anonymousId, timeZoneOffset, time);
        } catch (Exception e) {
            reportError(resp, 400, e.getMessage());
            return;
        }
        if (result.continueSession) {
            writeEmptySuccess(resp, 205);
        } else {
            writeEmptySuccess(resp, 204);
        }
    }

    private void handleRandomData(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AnalyticsDatabase db = getDatabase(req);
        db.generateRandomData();
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

    private void writeEmptySuccess(HttpServletResponse resp, int status) throws IOException {
        resp.setStatus(status);
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


    private void handleAnalyseDay(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DatastoreAnalyticsAccess db = new DatastoreAnalyticsAccess();
        String date = req.getParameter("date");

        try {
            db.analyseDay(date);
        } catch (Exception e) {
            log.severe("Daily computation error " + e.getMessage());
            reportError(resp, 400, e.getMessage());
            return;
        }
        resp.setStatus(204);
    }

    private void handleAnalyseWeek(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DatastoreAnalyticsAccess db = new DatastoreAnalyticsAccess();
        String date = req.getParameter("date");
        try {
            db.analyseWeek(date);
        } catch (Exception e) {
            log.severe("Daily computation error " + e.getMessage());
            reportError(resp, 400, e.getMessage());
            return;
        }
        resp.setStatus(204);
    }

    private void handleAnalyseMonth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DatastoreAnalyticsAccess db = new DatastoreAnalyticsAccess();
        String date = req.getParameter("date");
        try {
            db.analyseMonth(date);
        } catch (Exception e) {
            log.severe("Daily computation error " + e.getMessage());
            reportError(resp, 400, e.getMessage());
            return;
        }
        resp.setStatus(204);
    }
}
