package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;


import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by jarrod on 23/01/17.
 */

public class DatastoreAnalyticsAccess implements AnalyticsDatabase {

    private final int SESSION_TIMEOUT = 1800000;

    private static final Logger log = Logger.getLogger(DatastoreAnalyticsAccess.class.getName());
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();


    @Override
    public QueryResult queryAnalytics(String fromDate, String toDate) {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.UK);
        Date startDate;
        Date endDate;
        try {
            startDate = df.parse(fromDate);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new Error("The from date parameter is invalid: " + fromDate);
        }
        try {
            endDate = df.parse(toDate);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new Error("The end date parameter is invalid: " + toDate);
        }

        return queryAnalytics(startDate, endDate);
    }

    public QueryResult queryAnalytics(Date fromDate, Date toDate) {
        Filter downFilter = new FilterPredicate("date", FilterOperator.GREATER_THAN_OR_EQUAL, fromDate);
        Filter upFilter = new FilterPredicate("date", FilterOperator.LESS_THAN_OR_EQUAL, toDate);
        Filter queryFilter = CompositeFilterOperator.and(downFilter, upFilter);

        Query q = new Query("ArcSession")
                .setFilter(queryFilter)
                .addProjection(new PropertyProjection("appId", String.class));
        List<Entity> list = datastore.prepare(q)
                .asList(FetchOptions.Builder.withDefaults());

        ArrayList<String> uids = new ArrayList<>();
        long users = 0;
        long sessions = 0;

        for (Entity item: list) {
            String appId = (String) item.getProperty("appId");
            if (!uids.contains(appId)) {
                uids.add(appId);
                users++;
            }
            sessions++;
        }

        QueryResult result = new QueryResult();
        result.sessions = sessions;
        result.users = users;
        result.startDate = fromDate;
        result.endDate = toDate;
        return result;
    }

    @Override
    public InsertResult recordSession(String applicationId, Integer timeZoneOffset, Long recordedDate) {
        // Do not accept client timestamp since it can't be reliable
        Date d = new Date();
        Long time = d.getTime();
        time += timeZoneOffset;
        Date currentDate = new Date(time);

        Entity existing = getActiveSession(applicationId, time);

        InsertResult insert = new InsertResult();
        insert.success = true;
        if (existing != null) {
            existing.setProperty("lastUpdate", currentDate);
            datastore.put(existing);
            insert.continueSession = true;
            return insert;
        }
        Entity arcSession = new Entity("ArcSession");
        arcSession.setProperty("appId", applicationId);
        arcSession.setProperty("date", currentDate);
        arcSession.setProperty("lastUpdate", currentDate);
        datastore.put(arcSession);
        insert.continueSession = false;
        return insert;
    }

    @Override
    public void generateRandomData() {
        int size = 2000;
        ArrayList<Entity> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Date d = getRandomDay();
            Entity arcSession = new Entity("ArcSession");
            arcSession.setProperty("appId", UUID.randomUUID().toString());
            arcSession.setProperty("date", d);
            arcSession.setProperty("lastUpdate", d);
            list.add(arcSession);
        }
        datastore.put(list);
    }

    private Entity getActiveSession(String applicationId, long recordedTime) {
        long past = recordedTime - SESSION_TIMEOUT;
        Date datePast = new Date(past);

        Filter appIdFilter = new FilterPredicate("appId", FilterOperator.EQUAL, applicationId);
        Filter timeFilter = new FilterPredicate("lastUpdate", FilterOperator.GREATER_THAN_OR_EQUAL, datePast);
        Filter queryFilter = CompositeFilterOperator.and(appIdFilter, timeFilter);

        Query q = new Query("ArcSession")
            .setFilter(queryFilter)
            .addSort("lastUpdate", SortDirection.ASCENDING);
        List<Entity> results = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1));
        if (results.size() == 1) {
            return results.get(0);
        }
        log.warning("NOT FOUND IN  getActiveSession " + applicationId + ", " + recordedTime);
        return null;
    }

    private Date getRandomDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.DATE, randBetween(cal.getActualMinimum(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH)));
        return cal.getTime();
    }

    private int randBetween(int start, int end) {
        return start + (int)Math.round(Math.random() * (end - start));
    }

    /**
     * Analyse a single day and store data with number of sessions and users during this day.
     *
     * @param date A day date as a YYYY-mm-ddd
     */
    public void analyseDay(String date) throws Exception {
        Date start;
        Date end;
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        if (date != null) {
            Date d = df.parse(date);
            cal.setTime(d);
        } else {
            cal.add(Calendar.DAY_OF_MONTH, -1); // Count data for yesterday.
        }
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        start = cal.getTime();

        String entryKey = df.format(start);

        // First check if result record exists
        try {
            datastore.get(KeyFactory.createKey("DailyAnalytics", entryKey));
            // No reason to redo the work.
            log.info("DailyAnalytics task was performed for the day of " + start);
            return;
        } catch (EntityNotFoundException e) {
            // No entry, proceed
        }

        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMaximum(Calendar.MILLISECOND));
        end = cal.getTime();

        DateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
        log.info("Generating daily report " + logFormat.format(start) + " - " + logFormat.format(end));

        QueryResult result = queryAnalytics(start, end);
        Entity info = new Entity("DailyAnalytics", entryKey);
        info.setProperty("sessions", result.sessions);
        info.setProperty("users", result.users);
        datastore.put(info);
    }

    /**
     * Analyse a single week and store data with number of sessions and users during this week.
     *
     * @param date A day when the week starts as a YYYY-mm-dd. This function will search for previous
     *             first day of week (if pointed date isn't one)
     */
    public void analyseWeek(String date) throws Exception {
        Date start;
        Date end;
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        if (date != null) {
            Date d = df.parse(date);
            cal.setTime(d);
        } else {
            cal.add(Calendar.DAY_OF_MONTH, -7); // Count data for last week.
        }
        // set previous monday
        int day = cal.get(Calendar.DAY_OF_WEEK);
        int firstDayOfWeek = cal.getFirstDayOfWeek();
        while (day != firstDayOfWeek) {
            cal.add(Calendar.DATE, -1);
            day = cal.get(Calendar.DAY_OF_WEEK);
        }
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        start = cal.getTime();

        String entryKey = df.format(start);

        // First check if result record exists
        try {
            datastore.get(KeyFactory.createKey("WeeklyAnalytics", entryKey));
            // No reason to redo the work.
            log.info("WeeklyAnalytics task was performed for the week of " + start);
            return;
        } catch (EntityNotFoundException e) {
            // No entry, proceed
        }

        cal.add(Calendar.DAY_OF_MONTH, 6);
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMaximum(Calendar.MILLISECOND));
        end = cal.getTime();

        DateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK);
        log.info("Generating weekly report " + logFormat.format(start) + " - " + logFormat.format(end));

        QueryResult result = queryAnalytics(start, end);

        Entity info = new Entity("WeeklyAnalytics", entryKey);
        info.setProperty("sessions", result.sessions);
        info.setProperty("users", result.users);
        datastore.put(info);
    }

    /**
     * Analyse a single month and store data with number of sessions and users during this month.
     *
     * @param date A data as a YYYY-mm
     */
    public void analyseMonth(String date) throws Exception {
        Date start;
        Date end;
        Calendar cal = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat("yyyy-MM");
        if (date != null) {
            log.info("Got date to use as a reference: " + date);
            Date d = df.parse(date);
            cal.setTime(d);
        } else {
            cal.add(Calendar.MONTH, -1); // Count data for last month.
        }
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMinimum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMinimum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMinimum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMinimum(Calendar.MILLISECOND));
        start = cal.getTime();

        String entryKey = df.format(start);

        // First check if result record exists
        try {
            datastore.get(KeyFactory.createKey("MonthlyAnalytics", entryKey));
            // No reason to redo the work.
            log.info("MonthlyAnalytics task was performed for the month of " + start);
            return;
        } catch (EntityNotFoundException e) {
            // No entry, proceed
        }

        cal.add(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH)-1);
        cal.set(Calendar.HOUR_OF_DAY, cal.getActualMaximum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getActualMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getActualMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getActualMaximum(Calendar.MILLISECOND));
        end = cal.getTime();

        DateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("Generating monthly report " + logFormat.format(start) + " - " + logFormat.format(end));

        QueryResult result = queryAnalytics(start, end);

        Entity info = new Entity("MonthlyAnalytics", entryKey);
        info.setProperty("sessions", result.sessions);
        info.setProperty("users", result.users);
        datastore.put(info);
    }
}
