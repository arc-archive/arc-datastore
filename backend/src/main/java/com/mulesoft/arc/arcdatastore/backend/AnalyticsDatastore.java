package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;


import com.google.appengine.api.datastore.QueryResultList;
import com.google.common.primitives.Ints;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsDailyItemResult;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsDailySessions;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsDailyUsers;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsMonthlySessions;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsMonthlyUsers;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsWeeklySessions;
import com.mulesoft.arc.arcdatastore.backend.models.ArcAnalyticsWeeklyUsers;
import com.mulesoft.arc.arcdatastore.backend.models.SessionsComputationResult;
import com.mulesoft.arc.arcdatastore.backend.models.UsersComputationResult;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * A database access implementation for the analytics data.
 * It represents low level data access.
 */

class AnalyticsDatastore {

    private static final Logger log = Logger.getLogger(AnalyticsDatastore.class.getName());
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    /**
     * Records user and user's session in analytics datastore.
     *
     * @param applicationId The client generated Application ID.
     * @param timeZoneOffset Client's timezone offset.
     * @return True if new session has been recorded or false if existing session has been updated.
     */
    boolean recordSession(String applicationId, Integer timeZoneOffset) {
        Date d = new Date();
        Long time = d.getTime();
        time += timeZoneOffset;

        Entity existingUser = getActiveUser(applicationId);
        Entity existingSession = getActiveSession(applicationId, time);

        if (existingUser == null) {
            createActiveUser(applicationId);
        }

        if (existingSession == null) {
            createActiveSession(applicationId, time);
            return true;
        } else {
            updateActiveSession(existingSession, time);
            return false;
        }
    }

    /**
     * For development purpose.
     * Generates random data in the store.
     */
    void generateRandomData() {

        SimpleDateFormat df = new SimpleDateFormat("YYYYMMdd", Locale.UK);
        int size = 10000;
        ArrayList<Entity> list = new ArrayList<>(size);
        String[] appids = new String[size];

        for (int i = 0; i < size; i++) {
            Long d = getRandomDay();
            String appId = UUID.randomUUID().toString();
            appids[i] = appId;

            Entity arcSession = new Entity("Session");
            arcSession.setProperty("appId", appId);
            arcSession.setProperty("day", d);
            arcSession.setProperty("lastActive", d);
            list.add(arcSession);
        }
        size = (int) Math.round(size * 0.7);
        ArrayList<Entity> listUsers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Long d = getRandomDay();
            String day = df.format(new Date(d));
            String appId = appids[i];
            Key key = KeyFactory.createKey("User", appId + "/" + day);
            Entity user = new Entity(key);
            user.setProperty("appId", appId);
            user.setProperty("day", d);
            listUsers.add(user);
        }

        datastore.put(list);
        datastore.put(listUsers);

        log.warning("Generated random data.");
    }

    /**
     * Gets the User entity saved this day for given applicationId.
     *
     * @param applicationId The app id.
     * @return Entity associated with the app ID and current day (meaning the user has been
     * already recorded this day) or null if this if first user's visit today.
     */
    private Entity getActiveUser(String applicationId) {
        SimpleDateFormat df = new SimpleDateFormat("YYYYMMdd", Locale.UK);
        String date = df.format(new Date());

        Key key = KeyFactory.createKey("User", applicationId + "/" + date);
        try {
            return datastore.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private void createActiveUser(String applicationId) {
        SimpleDateFormat df = new SimpleDateFormat("YYYYMMdd", Locale.UK);
        Date now = new Date();
        String date = df.format(now);

        Key key = KeyFactory.createKey("User", applicationId + "/" + date);
        Entity user = new Entity(key);
        user.setProperty("appId", applicationId);
        user.setProperty("day", now.getTime());

        datastore.put(user);
    }

    /**
     * Gets active user's session.
     * The session is valid for 30 minutes since last activity represented as the lastActive
     * property of the entity.
     *
     * @param applicationId An application ID.
     * @param recordedTime Recorded time of user activity taking timezone into account.
     * @return An entity for given app ID and for lastActive time greater or equals recordedTime
     * minus 30 minutes.
     */
    private Entity getActiveSession(String applicationId, long recordedTime) {
        long past = recordedTime - 1800000;

        Filter appIdFilter = new FilterPredicate("appId", FilterOperator.EQUAL, applicationId);
        Filter timeFilter = new FilterPredicate("lastActive", FilterOperator.GREATER_THAN_OR_EQUAL, past);
        Filter queryFilter = CompositeFilterOperator.and(appIdFilter, timeFilter);

        Query q = new Query("Session")
            .setFilter(queryFilter)
            .addSort("lastActive", SortDirection.DESCENDING);
        List<Entity> results = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1));
        if (results != null && results.size() == 1) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Creates a record for new user's session.
     * Session are separated from the Users and are different not related objects.
     *
     * @param applicationId The application ID
     * @param time computed session timestamp taking timezone into the account.
     */
    private void createActiveSession(String applicationId, long time) {
        Entity arcSession = new Entity("Session");
        arcSession.setProperty("appId", applicationId);
        arcSession.setProperty("day", time);
        arcSession.setProperty("lastActive", time);
        datastore.put(arcSession);
    }

    /**
     * Updates existing session obbject setting lastActive to recorded time
     * @param existingSession Existing user's session object.
     * @param time Recorded time of user activity taking timezone into account.
     */
    private void updateActiveSession(Entity existingSession, Long time) {
        existingSession.setProperty("lastActive", time);
        datastore.put(existingSession);
    }

    /**
     * Generates random day for last month.
     * @return A timestamp of random day.
     */
    private Long getRandomDay() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        int random = randBetween(0, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        if (random > 0) {
            cal.add(Calendar.DATE, random);
        }
        return cal.getTimeInMillis();
    }

    private int randBetween(int start, int end) {
        return start + (int)Math.round(Math.random() * (end - start));
    }

    /**
     * Computes the data for daily sessions.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    boolean analyseDailySessions(long start, long end) throws ComputationRecordExistsException {
        return computeSessions(start, end, "yyyy-MM-dd", "DailySessions");
    }
    /**
     * Computes the data for weekly sessions.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    boolean analyseWeeklySessions(long start, long end) throws ComputationRecordExistsException {
        return computeSessions(start, end, "yyyy-MM-dd", "WeeklySessions");
    }
    /**
     * Computes the data for monthly sessions.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    boolean analyseMonthlySessions(long start, long end) throws ComputationRecordExistsException {
        return computeSessions(start, end, "yyyy-MM", "MonthlySessions");
    }

    /**
     * Performs a query on Session store.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @param keyFormat Database key format as a Date format
     * @param groupName The storage name where to save the result.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    private boolean computeSessions(long start, long end, String keyFormat, String groupName) throws ComputationRecordExistsException {
        DateFormat df = new SimpleDateFormat(keyFormat, Locale.UK);
        String entryKey = df.format(new Date(start));

        // First check if result record exists
        try {
            datastore.get(KeyFactory.createKey(groupName, entryKey));
            throw new ComputationRecordExistsException();
        } catch (EntityNotFoundException e) {
            // No entry, proceed
        }

        AnalyticsSessionsQuery asq = new AnalyticsSessionsQuery(start, end);
        SessionsComputationResult result = asq.query();
        if (result == null) {
            // interrupted by limit
            return false;
        }

        Entity info = new Entity(groupName, entryKey);
        info.setProperty("day", start);
        info.setProperty("sessions", result.sessions);

        datastore.put(info);

        return true;
    }

    /**
     * Computes the data for daily users.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    boolean analyseDailyUsers(long start, long end) throws ComputationRecordExistsException {
        return computeUsers(start, end, "yyyy-MM-dd", "DailyUsers");
    }
    /**
     * Computes the data for weekly users.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    boolean analyseWeeklyUsers(long start, long end) throws ComputationRecordExistsException {
        return computeUsers(start, end, "yyyy-MM-dd", "WeeklyUsers");
    }
    /**
     * Computes the data for monthly users.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    boolean analyseMonthlyUsers(long start, long end) throws ComputationRecordExistsException {
        return computeUsers(start, end, "yyyy-MM", "MonthlyUsers");
    }
    /**
     * Performs a query on Users store.
     *
     * @param start A long representing a midnight for the computation day.
     * @param end A long representing a millisecond before midnight for the last computation day.
     * @param keyFormat Database key format as a Date format
     * @param groupName The storage name where to save the result.
     * @return true if the computation succeeded and false if there was an error.
     * @throws ComputationRecordExistsException
     */
    private boolean computeUsers(long start, long end, String keyFormat, String groupName) throws ComputationRecordExistsException {
        DateFormat df = new SimpleDateFormat(keyFormat, Locale.UK);
        String entryKey = df.format(new Date(start));

        // First check if result record exists
        try {
            datastore.get(KeyFactory.createKey(groupName, entryKey));
            throw new ComputationRecordExistsException();
        } catch (EntityNotFoundException e) {
            // No entry, proceed
        }

        AnalyticsUserQuery auq = new AnalyticsUserQuery(start, end);
        UsersComputationResult result = auq.query();
        if (result == null) {
            // interrupted by limit
            return false;
        }

        Entity info = new Entity(groupName, entryKey);
        info.setProperty("day", start);
        info.setProperty("users", result.users);

        datastore.put(info);

        return true;
    }

    ArcAnalyticsDailyUsers getDailyUser(long start) throws EntityNotFoundException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        String entryKey = df.format(new Date(start));

        Key key = KeyFactory.createKey("DailyUsers", entryKey);
        Entity dailyUsers = datastore.get(key);
        ArcAnalyticsDailyUsers result = new ArcAnalyticsDailyUsers();
        result.result = Ints.saturatedCast((Long) dailyUsers.getProperty("users"));
        return result;
    }
    ArcAnalyticsWeeklyUsers getWeeklyUsers(long start, long end) throws EntityNotFoundException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        String entryKey = df.format(new Date(start));

        Key key = KeyFactory.createKey("WeeklyUsers", entryKey);
        Entity weeklyUsers = datastore.get(key);
        ArcAnalyticsWeeklyUsers result = new ArcAnalyticsWeeklyUsers(entryKey);
        result.result = Ints.saturatedCast((Long) weeklyUsers.getProperty("users"));

        // Query for daily items.
        result.items = queryDailyUsers(start, end);

        return result;
    }
    ArcAnalyticsMonthlyUsers getMonthlyUsers(long start, long end) throws EntityNotFoundException {
        Date startDate = new Date(start);
        DateFormat df = new SimpleDateFormat("yyyy-MM", Locale.UK);
        String entryKey = df.format(startDate);

        Key key = KeyFactory.createKey("MonthlyUsers", entryKey);
        Entity monthlyUsers = datastore.get(key);
        df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        ArcAnalyticsMonthlyUsers result = new ArcAnalyticsMonthlyUsers(df.format(startDate));
        result.result = Ints.saturatedCast((Long) monthlyUsers.getProperty("users"));

        // Query for daily items.
        result.items = queryDailyUsers(start, end);

        return result;
    }

    ArcAnalyticsDailySessions getDailySession(long start) throws EntityNotFoundException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        String entryKey = df.format(new Date(start));

        Key key = KeyFactory.createKey("DailySessions", entryKey);
        Entity dailySessions = datastore.get(key);
        ArcAnalyticsDailySessions result = new ArcAnalyticsDailySessions();
        result.result = Ints.saturatedCast((Long) dailySessions.getProperty("sessions"));
        return result;
    }

    ArcAnalyticsWeeklySessions getWeeklySessions(long start, long end) throws EntityNotFoundException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        String entryKey = df.format(new Date(start));

        Key key = KeyFactory.createKey("WeeklySessions", entryKey);
        Entity weeklyUsers = datastore.get(key);
        ArcAnalyticsWeeklySessions result = new ArcAnalyticsWeeklySessions(entryKey);
        result.result = Ints.saturatedCast((Long) weeklyUsers.getProperty("sessions"));
        // Query for daily items.
        result.items = queryDailySessions(start, end);

        return result;
    }

    ArcAnalyticsMonthlySessions getMonthlySessions(long start, long end) throws EntityNotFoundException {
        Date startDate = new Date(start);
        DateFormat df = new SimpleDateFormat("yyyy-MM", Locale.UK);
        String entryKey = df.format(startDate);

        Key key = KeyFactory.createKey("MonthlySessions", entryKey);
        Entity monthlyUsers = datastore.get(key);
        df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        ArcAnalyticsMonthlySessions result = new ArcAnalyticsMonthlySessions(df.format(startDate));
        result.result = Ints.saturatedCast((Long) monthlyUsers.getProperty("sessions"));

        // Query for daily items.
        result.items = queryDailySessions(start, end);

        return result;
    }

    private ArcAnalyticsDailyItemResult[] queryDailySessions(long start, long end) {
        FetchOptions fetchOptions = FetchOptions.Builder.withDefaults();

        Query.Filter fromTimeFilter = new Query.FilterPredicate("day", Query.FilterOperator.GREATER_THAN_OR_EQUAL, start);
        Query.Filter toTimeFilter = new Query.FilterPredicate("day", Query.FilterOperator.LESS_THAN_OR_EQUAL, end);
        Query.Filter timeRangeFilterFilter = Query.CompositeFilterOperator.and(fromTimeFilter, toTimeFilter);

        Query q = new Query("DailySessions")
                .setFilter(timeRangeFilterFilter);

        PreparedQuery pq = datastore.prepare(q);
        QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
        int size = results.size();
        ArcAnalyticsDailyItemResult[] items = new ArcAnalyticsDailyItemResult[size];
        int i = 0;
        for (Entity entity: results) {
            ArcAnalyticsDailyItemResult item = new ArcAnalyticsDailyItemResult();
            item.day = entity.getKey().getName();
            item.value = Ints.saturatedCast((Long) entity.getProperty("sessions"));
            items[i] = item;
            i++;
        }
        Arrays.sort(items);
        return items;
    }

    private ArcAnalyticsDailyItemResult[] queryDailyUsers(long start, long end) {
        FetchOptions fetchOptions = FetchOptions.Builder.withDefaults();

        Query.Filter fromTimeFilter = new Query.FilterPredicate("day", Query.FilterOperator.GREATER_THAN_OR_EQUAL, start);
        Query.Filter toTimeFilter = new Query.FilterPredicate("day", Query.FilterOperator.LESS_THAN_OR_EQUAL, end);
        Query.Filter timeRangeFilterFilter = Query.CompositeFilterOperator.and(fromTimeFilter, toTimeFilter);

        Query q = new Query("DailyUsers")
                .setFilter(timeRangeFilterFilter);

        PreparedQuery pq = datastore.prepare(q);
        QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
        int size = results.size();
        ArcAnalyticsDailyItemResult[] items = new ArcAnalyticsDailyItemResult[size];
        int i = 0;
        for (Entity entity: results) {
            ArcAnalyticsDailyItemResult item = new ArcAnalyticsDailyItemResult();
            item.day = entity.getKey().getName();
            item.value = Ints.saturatedCast((Long) entity.getProperty("users"));
            items[i] = item;
            i++;
        }
        Arrays.sort(items);
        return items;
    }
}
