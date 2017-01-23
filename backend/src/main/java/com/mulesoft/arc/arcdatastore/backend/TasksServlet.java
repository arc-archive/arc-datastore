package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.gson.Gson;
import com.mulesoft.arc.arcdatastore.backend.models.ErrorResponse;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet called by cron to add tasks to queue and lease the tasks.
 */

public class TasksServlet extends HttpServlet {
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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Queue q = QueueFactory.getQueue("analytics-analyser");

        int todayDay = cal.get(Calendar.DAY_OF_MONTH);
        int todayWeekday = cal.get(Calendar.DAY_OF_WEEK);

        cal.add(Calendar.DAY_OF_MONTH, -1);
        // Always add daily task.
        String date = df.format(cal.getTime());
        q.add(TaskOptions.Builder.withUrl("/analytics/analyse-day")
                .param("date", date)
                .taskName("daily-analyser-" + date)
                .method(TaskOptions.Method.GET));

        log.info("Scheduled daily task.");
        if (todayWeekday == cal.getFirstDayOfWeek()) {
            cal.add(Calendar.DAY_OF_MONTH, -6); // not 7 because 1 day was already subtracted.
            // schedule last week
            date = df.format(cal.getTime());
            q.add(TaskOptions.Builder.withUrl("/analytics/analyse-week")
                    .param("date", date)
                    .taskName("weekly-analyser-" + date)
                    .method(TaskOptions.Method.GET));
            cal.add(Calendar.DAY_OF_MONTH, 6);
            log.info("Scheduled weekly task.");
        }

        if (todayDay == 1) {
            cal.add(Calendar.MONTH, -1);
            // schedule last month.
            df = new SimpleDateFormat("yyyy-MM");
            date = df.format(cal.getTime());
            q.add(TaskOptions.Builder.withUrl("/analytics/analyse-month")
                    .param("date", date)
                    .taskName("monthly-analyser")
                    .method(TaskOptions.Method.GET));
            log.info("Scheduled monthly task.");
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
            error = error.concat("The date parameter is required. Set it to the date in format YYYY-mm-dd for \"daily\" and \"weekly\" types and YYYY-mm for \"monthly\" type.");
        }

        if (!error.equals("")) {
            reportError(resp, 400, error);
            return;
        }

        String dateFormat = "YYY-mm";
        if (!type.equals("monthly")) {
            dateFormat = dateFormat.concat("-dd");
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setLenient(false);

        try {
            sdf.parse(date);
        } catch (ParseException e) {
            reportError(resp, 400, "The date parameter has invalid format. Please, use date in format YYYY-mm-dd for \"daily\" and \"weekly\" types and YYYY-mm for \"monthly\" type.");
            return;
        }

        String url = "/analytics/analyse-";
        String taskName;
        if (type.equals("daily")) {
            url = url.concat("day");
            taskName = "daily";
        } else if (type.equals("weekly")) {
            url = url.concat("week");
            taskName = "weekly";
        } else {
            url = url.concat("month");
            taskName = "monthly";
        }
        taskName = taskName.concat("-analyser-" + date);
        Queue q = QueueFactory.getQueue("analytics-analyser");
        q.add(TaskOptions.Builder.withUrl(url)
                .param("date", date)
                .taskName(taskName)
                .method(TaskOptions.Method.GET));
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
}
