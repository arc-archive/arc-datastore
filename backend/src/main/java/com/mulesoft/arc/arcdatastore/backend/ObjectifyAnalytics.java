package com.mulesoft.arc.arcdatastore.backend;

import com.google.appengine.api.datastore.Entity;
import com.google.gson.Gson;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.cmd.Query;
import com.mulesoft.arc.arcdatastore.backend.models.ArcSession;
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

/**
 * Created by jarrod on 23/01/17.
 */

public class ObjectifyAnalytics implements AnalyticsDatabase {

    private final int SESSION_TIMEOUT = 1800000;

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

        long users = 0;
        long sessions = 0;
        ArrayList<String> uids = new ArrayList<>();

        Objectify ofy = OfyService.ofy();
        Query<ArcSession> q = ofy.load().type(ArcSession.class)
                .filter("date >=", startDate);
        List<ArcSession> list = q.list();
        for (ArcSession s : list) {
            String uid = s.appId;
            Date date = s.date;
            if(endDate.after(s.date) == false) {
                continue;
            }
            if (!uids.contains(uid)) {
                users++;
                uids.add(uid);
            }
            sessions++;
        }
//        QueryKeys<ArcSession> keys = q.keys();
//        log.info("Has list of keys for date >= sd " + startDate.toString());
//        log.info("Attempting to make another query");
//
//        q = ofy.load().type(ArcSession.class).filter("date <=", endDate);
//        if (keys.list().size() > 0) {
//            q = q.filterKey("in", keys);
//        }
//        log.info("Attempting to make another query: " + q.toString());
//        List<ArcSession> list = q.list();
//
//
//
//        log.info("Attempting to make another query: " + q.toString());
//        for (ArcSession s : list) {
//            String uid = s.appId;
//            if (!uids.contains(uid)) {
//                users++;
//                uids.add(uid);
//            }
//            sessions++;
//        }

        QueryResult result = new QueryResult();
        result.sessions = sessions;
        result.users = users;
        result.startDate = startDate;
        result.endDate = endDate;
        return result;
    }

    @Override
    public InsertResult recordSession(String applicationId, Integer timeZoneOffset, String recordedDate) {
        // Do not accept client timestamp since it can't be reliable
        Date d = new Date();
        Long time = d.getTime();

        time += timeZoneOffset; // Move to user's timezone.
        long past = time - SESSION_TIMEOUT; // 30 * 60 * 1000; - 30 minutes
        Date datePast = new Date(past);

        InsertResult insert = new InsertResult();
        insert.success = true;

        Objectify ofy = OfyService.ofy();

        ArcSession last = ofy.load().type(ArcSession.class)
        .filter("appId", applicationId)
        .filter("lastUpdate >=", datePast)
        .first().now();

        if (last != null) {
            // Still the same session, just update access time.
            insert.continueSession = true;
            last.lastUpdate = new Date(time);
            ofy.save().entity(last).now();
            return insert;
        }

        ArcSession rec = new ArcSession();
        rec.appId = applicationId;
        rec.date = new Date(time);
        rec.lastUpdate = rec.date;
        ofy.save().entity(rec).now();

        insert.continueSession = false;
        return insert;
    }

    @Override
    public void generateRandomData() {
        int size = 10000;
        ArrayList<Entity> list = new ArrayList<>(size);
        ArrayList<String> uuids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String uuid;
            int uuidsSize = uuids.size();
            if (uuidsSize > 10 && randBetween(1, 100) % 2 == 0) {
                uuid = uuids.get(randBetween(0, uuidsSize - 1));
            } else {
                uuid = UUID.randomUUID().toString();
                uuids.add(uuid);
            }

            Date d = getRandomDay();
            Entity arcSession = new Entity("ArcSession");
            arcSession.setProperty("appId", uuid);
            arcSession.setProperty("date", d);
            arcSession.setProperty("lastUpdate", d);
            list.add(arcSession);
        }
        Objectify ofy = OfyService.ofy();
        ofy.save().entities(list).now();
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
