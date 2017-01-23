package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;
import java.text.DateFormat;
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
}
