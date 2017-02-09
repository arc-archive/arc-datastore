package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A class representing Daily Sessions type.
 */

public class ArcAnalyticsMonthlySessions extends ArcAnalyticsRangeResult {

    public ArcAnalyticsMonthlySessions(String day) {
        super("ArcAnalytics#MonthlySessions", day);
    }
}
