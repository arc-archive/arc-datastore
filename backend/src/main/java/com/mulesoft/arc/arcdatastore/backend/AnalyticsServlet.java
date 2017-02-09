package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsDailyResult;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsRangeResult;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet to handle analytics data from clients and to query for aggregated data.
 */
@SuppressWarnings("serial")
public class AnalyticsServlet extends BaseServlet {

    private static final Logger log = Logger.getLogger(AnalyticsServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/".equals(path)) {
            resp.sendRedirect("/analytics.html");
        } else if (path != null && path.indexOf("/query") == 0) {
            handleQuery(path, req, resp);
        } else if ("/random".equals(path)) {
            String namespace = NamespaceManager.get();
            NamespaceManager.set("analytics");
            try {
                handleRandomData(resp);
            } finally {
                NamespaceManager.set(namespace);
            }
        } else {
            reportError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + path);
            log.warning("Unknown path for doGET: " + path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (!"/record".equals(path)) {
            reportError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + path);
            log.warning("Unknown path for doPost: " + path);
        }

        String namespace = NamespaceManager.get();
        NamespaceManager.set("analytics");
        try {
            handleRecord(req, resp);
        } finally {
            NamespaceManager.set(namespace);
        }
    }

    /**
     * Prepare data for analysis.
     * This request require sd (start date) and ed (end date) parameters.
     * As a result it will return an JSON object with number of users and sessions.
     *
     * @throws IOException
     */
    private void handleQuery(String path, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type = null;
        String scope = null;

        Pattern r = Pattern.compile("^/query/(daily|weekly|monthly)/(users|sessions)$");
        Matcher m = r.matcher(path);
        if (m.find()) {
            type = m.group(1);
            scope = m.group(2);
        }

        if (type == null || scope == null) {
            reportError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + path);
            log.warning("Unknown path for doGET: " + path);
        }

        String day = req.getParameter("day");
        if (day == null) {
            reportError(resp, 400, "The day parameter is required. Set day in yyyy-MM-dd format.");
            return;
        }

        try {
            validateDate(type, day);
        } catch (IllegalArgumentException e) {
            reportError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        String namespace = NamespaceManager.get();
        NamespaceManager.set("analytics");
        try {
            runQuery(type, scope, resp);
        } finally {
            NamespaceManager.set(namespace);
        }
    }

    private void runQuery(String type, String scope, HttpServletResponse resp) throws IOException {
        if ("daily".equals(type)) {
            queryDaily(scope, resp);
        } else if ("weekly".equals(type)) {
            queryWeekly(scope, resp);
        } else {
            queryMonthly(scope, resp);
        }
    }

    private void queryDaily(String scope, HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        ArcAnalyticsDailyResult result;
        try {
            if ("sessions".equals(scope)) {
                result = db.getDailySession(startTime);
            } else {
                result = db.getDailyUser(startTime);
            }
        } catch (EntityNotFoundException e) {
            reportError(resp, 404, "Not found. " + e.getMessage());
            return;
        }
        writeSuccess(resp, 200, result);
    }

    private void queryWeekly(String scope, HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        ArcAnalyticsRangeResult result;
        try {
            if ("sessions".equals(scope)) {
                result = db.getWeeklySessions(startTime, endTime);
            } else {
                result = db.getWeeklyUsers(startTime, endTime);
            }
        } catch (EntityNotFoundException e) {
            reportError(resp, 404, "Not found" + e.getMessage());
            return;
        }
        writeSuccess(resp, 200, result);
    }

    private void queryMonthly(String scope, HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        ArcAnalyticsRangeResult result;
        try {
            if ("sessions".equals(scope)) {
                result = db.getMonthlySessions(startTime, endTime);
            } else {
                result = db.getMonthlyUsers(startTime, endTime);
            }
        } catch (EntityNotFoundException e) {
            reportError(resp, 404, "Not found" + e.getMessage());
            return;
        }
        writeSuccess(resp, 200, result);
    }

    /**
     * Record a session.
     *
     * @throws IOException
     */
    private void handleRecord(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String apiVersion = req.getHeader("x-api-version");
        if ("2".equals(apiVersion)) {
            handleRecordV2(req, resp);
        } else {
            reportError(resp, 400, "Unsupported API version");
        }
    }

    private void handleRecordV2(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] allowed = { "aid", "tz" };

        String tz = null;
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
        if (tz == null) {
            message = message.concat("The `tz` (timeZoneOffset) parameter is required. ");
        }

        if (!message.equals("") || tz == null) {
            reportError(resp, 400, message);
            return;
        }

        Integer timeZoneOffset = null;
        try {
            timeZoneOffset = Integer.parseInt(tz);
        } catch (Exception e) {
            message = "'tz' (timeZoneOffset) is invalid: " + tz + ". Expecting integer.";
        }

        if (!message.equals("")) {
            reportError(resp, 400, message);
            return;
        }

        AnalyticsDatastore db = new AnalyticsDatastore();
        Boolean result;
        try {
            result = db.recordSession(anonymousId, timeZoneOffset);
        } catch (Exception e) {
            reportError(resp, 400, e.getMessage());
            return;
        }
        if (result) {
            // New session.
            writeEmptySuccess(resp, 204);
        } else {
            // Updating existing session.
            writeEmptySuccess(resp, 205);
        }
    }

    private void handleRandomData(HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        db.generateRandomData();
        resp.setStatus(204);
    }
}
