package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet called by cron to add tasks to queue and lease the tasks.
 */

public class TasksServlet extends BaseServlet {
    private static final Logger log = Logger.getLogger(TasksServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/schedule-analyser".equals(path)) {
            addTasks(resp);
            return;
        } else if ("/schedule".equals(path)) {
            addTasksByType(req, resp);
            return;
        }

        resp.setStatus(400);
    }

    private void addTasks(HttpServletResponse resp) {
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

        String namespace = NamespaceManager.get();
        NamespaceManager.set("analytics");
        Queue q = QueueFactory.getQueue("analytics-analyser");

        int todayDay = cal.get(Calendar.DAY_OF_MONTH);
        int todayWeekday = cal.get(Calendar.DAY_OF_WEEK);

        cal.add(Calendar.DAY_OF_MONTH, -1);
        // Always add daily task.
        String date = df.format(cal.getTime());

        try {
            // Daily users
            q.add(TaskOptions.Builder.withUrl("/analyser/daily/users")
                    .param("date", date)
                    .method(TaskOptions.Method.GET));
            // Daily sessions
            q.add(TaskOptions.Builder.withUrl("/analyser/daily/sessions")
                    .param("date", date)
                    .method(TaskOptions.Method.GET));

            if (todayWeekday == cal.getFirstDayOfWeek()) {
                cal.add(Calendar.DAY_OF_MONTH, -6); // not 7 because 1 day was already subtracted.
                // schedule last week
                date = df.format(cal.getTime());

                // Weekly users
                q.add(TaskOptions.Builder.withUrl("/analyser/weekly/users")
                        .param("date", date)
                        .method(TaskOptions.Method.GET));
                // Weekly sessions
                q.add(TaskOptions.Builder.withUrl("/analyser/weekly/sessions")
                        .param("date", date)
                        .method(TaskOptions.Method.GET));

                cal.add(Calendar.DAY_OF_MONTH, 6);
                log.info("Scheduled weekly task.");
            }

            if (todayDay == 1) {
                cal.add(Calendar.MONTH, -1);
                // schedule last month.
                date = df.format(cal.getTime());

                // Monthly users
                q.add(TaskOptions.Builder.withUrl("/analyser/monthly/users")
                        .param("date", date)
                        .method(TaskOptions.Method.GET));
                // Monthly sessions
                q.add(TaskOptions.Builder.withUrl("/analyser/monthly/sessions")
                        .param("date", date)
                        .method(TaskOptions.Method.GET));
            }
        } finally {
            NamespaceManager.set(namespace);
        }
        resp.setStatus(204);
    }

    private void addTasksByType(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String type = req.getParameter("type");
        String date = req.getParameter("date");
        String error = "";

        if (type == null) {
            error = "The type parameter is required. Set it to one of \"daily\", \"weekly\" or \"monthly\". ";
        } else if (!("daily".equals(type) || "weekly".equals(type) || "monthly".equals(type))) {
            error = "The type parameter is invalid (" + type + "). Set it to one of \"daily\", \"weekly\" or \"monthly\". ";
        }
        if (date == null) {
            error = error.concat("The date parameter is required. Set it to the date in format YYYY-MM-dd");
        }

        if (!error.equals("")) {
            reportError(resp, 400, error);
            return;
        }

        String dateFormat = "YYY-mm-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.UK);
        sdf.setLenient(false);

        try {
            sdf.parse(date);
        } catch (ParseException e) {
            reportError(resp, 400, "The date parameter has invalid format. Please, use date in format YYYY-MM-dd.");
            return;
        }

        if (date == null || type == null) {
            // it's just to make AndroidStudio's linter happy again.
            return;
        }

        String url = "/analyser/" + type + "/";

        String namespace = NamespaceManager.get();
        NamespaceManager.set("analytics");
        Queue q = QueueFactory.getQueue("analytics-analyser");

        try {
            // Users
            q.add(TaskOptions.Builder.withUrl(url + "users")
                    .param("date", date)
                    .method(TaskOptions.Method.GET));
            // Sessions
            q.add(TaskOptions.Builder.withUrl(url + "sessions")
                    .param("date", date)
                    .method(TaskOptions.Method.GET));
        } finally {
            NamespaceManager.set(namespace);
        }

        resp.setStatus(204);
    }
}
