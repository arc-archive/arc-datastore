package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A class representing Daily Sessions type.
 */

public class ArcAnalyticsWeeklySessions extends ArcAnalyticsRangeResult {

    public ArcAnalyticsWeeklySessions(String day) {
        super("ArcAnalytics#WeeklySessions", day);
    }
}
