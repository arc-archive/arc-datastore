/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.mulesoft.arc.arcdatastore.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;

import com.googlecode.objectify.Objectify;
import com.mulesoft.arc.arcdatastore.backend.models.ArcSession;
import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** An endpoint class we are exposing */
@Api(
  name = "arcSessions",
  version = "v1",
  description = "An API to manage ARC sessions.",
  namespace = @ApiNamespace(
    ownerDomain = "backend.arcdatastore.arc.mulesoft.com",
    ownerName = "backend.arcdatastore.arc.mulesoft.com",
    packagePath=""
  )
)
public class ArcSessions {

    // http://localhost:8080/_ah/api/arcSessions/v1/record/aaa/${now}

    @ApiMethod(name = "record", httpMethod = ApiMethod.HttpMethod.POST )
    public InsertResult record(@Named("ai") String appId, @Named("t") Long time) {
        Objectify ofy = OfyService.ofy();

        long past = time - 1800000; // 30 * 60 * 1000; - 30 minutes
        Date datePast = new Date(past);

        InsertResult r = new InsertResult();
        r.success = true;

        ArcSession last = ofy.load().type(ArcSession.class)
            .filter("appId", appId)
            .filter("date >=", datePast)
            .first().now();

        // Still the same session
        if (last != null) {
            r.continueSession = true;
            return r;
        }

        ArcSession rec = new ArcSession();
        rec.appId = appId;
        rec.date = new Date(time);
        ofy.save().entity(rec).now();

        r.continueSession = false;
        return r;
    }

    //http://localhost:8080/_ah/api/arcSessions/v1/query?startDate=2016-07-01T00%3A00%3A00.000%2B01%3A00&endDate=2016-07-30T23%3A59%3A59.000%2B01%3A00

    @ApiMethod(name = "query", httpMethod = ApiMethod.HttpMethod.GET, path = "query" )
    public QueryResult query(@Named("startDate") @Nullable Date startDate, @Named("endDate") @Nullable Date endDate) {

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

        return result;
    }
}
