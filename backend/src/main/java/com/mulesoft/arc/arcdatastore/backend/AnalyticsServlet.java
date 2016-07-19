package com.mulesoft.arc.arcdatastore.backend;

import com.google.gson.Gson;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;
import com.mulesoft.arc.arcdatastore.backend.models.ArcSession;
import com.mulesoft.arc.arcdatastore.backend.models.ErrorResponse;
import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    // Session timeout in milliseconds - 30 minutes.
    private static final int SESSION_TIMEOUT = 1800000; // 30 * 60 * 1000; - 30 minutes

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/query".equals(path)) {
            handleQuery(req, resp);
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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.UK);
        Date startDate;
        Date endDate;
        try {
            startDate = df.parse(sd);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            reportError(resp, 400, "'sd' (startDate) parameter is invalid: " + sd);
            return;
        }
        try {
            endDate = df.parse(ed);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            reportError(resp, 400, "'ed' (endDate) parameter is invalid: " + ed);
            return;
        }

        // End params parsing and check.

        Objectify ofy = OfyService.ofy();
        Query<ArcSession> q = ofy.load().type(ArcSession.class).filter("date >=", startDate);
        QueryKeys<ArcSession> keys = q.keys();

        q = ofy.load().type(ArcSession.class).filter("date <=", endDate);
        if (keys.list().size() > 0) {
            q = q.filterKey("in", keys);
        }
        List<ArcSession> list = q.list();

        long users = 0;
        long sessions = 0;
        ArrayList<String> uids = new ArrayList<>();

        for (ArcSession s : list) {
            String uid = s.appId;
            if (!uids.contains(uid)) {
                users++;
                uids.add(uid);
            }
            sessions++;
        }

        QueryResult result = new QueryResult();
        result.sessions = sessions;
        result.users = users;
        result.startDate = startDate;
        result.endDate = endDate;

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
//        String t = req.getParameter("t");
        String tz = req.getParameter("tz");
        if (appId == null) {
            reportError(resp, 400, "'ai' (appId) parameter is missing but it's required");
            return;
        }
//        if (t == null) {
//            reportError(resp, 400, "'t' (time) parameter is missing but it's required.");
//            return;
//        }
        if (tz == null) {
            reportError(resp, 400, "'tz' (timeZoneOffset) parameter is missing but it's required.");
            return;
        }

//        Long time;
//        try {
//            time = Long.parseLong(t);
//        } catch (Exception e) {
//            reportError(resp, 400, "'t' (time) parameter is invalid: " + t);
//            return;
//        }
        Integer timeZoneOffset;
        try {
            timeZoneOffset = Integer.parseInt(tz);
        } catch (Exception e) {
            reportError(resp, 400, "'tz' (time) timeZoneOffset is invalid: " + tz);
            return;
        }

        // Do not accept client timestamp since it can't be reliable
        Date d = new Date();
        Long time = d.getTime();

        // End params parsing and check.

        time += timeZoneOffset; // Move to user's timezone.
        long past = time - 1800000; // 30 * 60 * 1000; - 30 minutes
        Date datePast = new Date(past);

        InsertResult r = new InsertResult();
        r.success = true;

        Objectify ofy = OfyService.ofy();

        ArcSession last = ofy.load().type(ArcSession.class)
                .filter("appId", appId)
                .filter("lastUpdate >=", datePast)
                .first().now();

        Gson gson = new Gson();

        // Still the same session
        if (last != null) {
            r.continueSession = true;
            last.lastUpdate = new Date(time);
            ofy.save().entity(last).now();
            writeSuccess(resp, gson.toJson(r));
            return;
        }

        ArcSession rec = new ArcSession();
        rec.appId = appId;
        rec.date = new Date(time);
        rec.lastUpdate = rec.date;
        ofy.save().entity(rec).now();

        r.continueSession = false;
        writeSuccess(resp, gson.toJson(r));
    }

    private void reportError(HttpServletResponse resp, int statusCode, String message) throws IOException {
//        System.out.println("UUUUPPPPSSSSS: " + message);

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
}
