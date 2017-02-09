package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A class representing Daily Users type.
 */

public class ArcAnalyticsMonthlyUsers extends ArcAnalyticsRangeResult {

    public ArcAnalyticsMonthlyUsers(String day) {
        super("ArcAnalytics#MonthlyUsers", day);
    }
}
