package com.mulesoft.arc.arcdatastore.backend;

import com.mulesoft.arc.arcdatastore.backend.models.InsertResult;
import com.mulesoft.arc.arcdatastore.backend.models.QueryResult;

/**
 * Created by jarrod on 23/01/17.
 */

public interface AnalyticsDatabase {
    public QueryResult queryAnalytics(String fromDate, String toDate);
    public InsertResult recordSession(String applicationId, Integer timeZoneOffset, String recordedDate);
    public void generateRandomData();
}
