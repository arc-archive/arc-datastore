package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DateTime;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.Transaction;
import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by jarrod on 23/01/17.
 */

public class DatastoreAnalyticsAccess implements AnalyticsDatabase {

    private final int SESSION_TIMEOUT = 1800000;

    private static final Logger log = Logger.getLogger(AnalyticsServlet.class.getName());
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

        Filter downFilter = new FilterPredicate("date", FilterOperator.GREATER_THAN_OR_EQUAL, startDate);
        Filter upFilter = new FilterPredicate("date", FilterOperator.LESS_THAN_OR_EQUAL, endDate);
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
//        result.startDate = startDate;
//        result.endDate = endDate;
        return result;
    }

    @Override
    public InsertResult recordSession(String applicationId, Integer timeZoneOffset, String recordedDate) {
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
        log.info("INSERTED! :) " + arcSession.getKey().getId());
        insert.continueSession = false;
        return insert;
    }

    @Override
    public void generateRandomData() {
        int size = 1000;
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
}
