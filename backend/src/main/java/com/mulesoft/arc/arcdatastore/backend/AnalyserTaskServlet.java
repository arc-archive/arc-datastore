package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.NamespaceManager;
import com.google.apphosting.api.DeadlineExceededException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The AnalyserTaskServlet class is responsible for data computation for daily, weekly and
 * monthly statistics.
 *
 * This is meant to run as a service called from the task queue which have 10 minutes
 * execution time limit.
 *
 * Endpoint structure:
 * - /analyser/daily/users
 * - /analyser/daily/sessions
 * - /analyser/weekly/users
 * - /analyser/weekly/sessions
 * - /analyser/monthly/users
 * - /analyser/monthly/sessions
 */
public class AnalyserTaskServlet extends BaseServlet {

    private static final Logger log = Logger.getLogger(AnalyserTaskServlet.class.getName());



    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        String type = null;
        String scope = null;

        if (path != null) {
            Pattern r = Pattern.compile("^/(daily|weekly|monthly)/(users|sessions)$");
            Matcher m = r.matcher(path);
            if (m.find()) {
                type = m.group(1);
                scope = m.group(2);
            }
        }

        if (type == null || scope == null) {
            reportError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + path);
            log.warning("Unknown path for doGET: " + path);
        } else {
            String namespace = NamespaceManager.get();
            NamespaceManager.set("analytics");
            try {
                runService(type, scope, req, resp);
            } finally {
                NamespaceManager.set(namespace);
            }

        }
    }

    private void runService(String type, String scope, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String date = req.getParameter("date");

        try {
            validateDate(type, date);
        } catch (IllegalArgumentException e) {
            reportError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        // times are set.

        if ("daily".equals(type)) {
            computeDaily(scope, resp);
        } else if ("weekly".equals(type)) {
            computeWeekly(scope, resp);
        } else {
            computeMonthly(scope, resp);
        }
    }

    private void computeDaily(String scope, HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        boolean result = false;
        try {
            if ("users".equals(scope)) {
                result = db.analyseDailyUsers(startTime, endTime);
            } else {
                result = db.analyseDailySessions(startTime, endTime);
            }
        } catch (ComputationRecordExistsException e) {
            // Nothing is happening, just exit quietly.
        } catch (DeadlineExceededException e) {
            log.severe("Daily " + scope + " computation timeout");
            reportError(resp, 400, e.getMessage());
            return;
        } catch (Exception e) {
            log.severe("Daily " + scope + " computation error " + e.getMessage());
            reportError(resp, 400, e.getMessage());
            return;
        }
        if (result) {
            resp.setStatus(204);
        } else {
            resp.setStatus(400);
        }
    }

    private void computeWeekly(String scope, HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        boolean result = false;
        try {
            if ("users".equals(scope)) {
                result = db.analyseWeeklyUsers(startTime, endTime);
            } else {
                result = db.analyseWeeklySessions(startTime, endTime);
            }
        } catch (ComputationRecordExistsException e) {
            // Nothing is happening, just exit quietly.
        } catch (DeadlineExceededException e) {
            log.severe("Weekly " + scope + " computation timeout");
            reportError(resp, 400, e.getMessage());
            return;
        } catch (Exception e) {
            log.severe("Weekly " + scope + " computation error " + e.getMessage());
            reportError(resp, 400, e.getMessage());
            return;
        }
        if (result) {
            resp.setStatus(204);
        } else {
            resp.setStatus(400);
        }
    }

    private void computeMonthly(String scope, HttpServletResponse resp) throws IOException {
        AnalyticsDatastore db = new AnalyticsDatastore();
        boolean result = false;
        try {
            if ("users".equals(scope)) {
                result = db.analyseMonthlyUsers(startTime, endTime);
            } else {
                result = db.analyseMonthlySessions(startTime, endTime);
            }
        } catch (ComputationRecordExistsException e) {
            // Nothing is happening, just exit quietly.
        } catch (DeadlineExceededException e) {
            log.severe("Weekly " + scope + " computation timeout");
            reportError(resp, 400, e.getMessage());
            return;
        } catch (Exception e) {
            log.severe("Weekly " + scope + " computation error " + e.getMessage());
            reportError(resp, 400, e.getMessage());
            return;
        }
        if (result) {
            resp.setStatus(204);
        } else {
            resp.setStatus(400);
        }
    }


}
