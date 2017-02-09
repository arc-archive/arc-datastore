package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A class representing Daily Users type.
 */

public class ArcAnalyticsWeeklyUsers extends ArcAnalyticsRangeResult {

    public ArcAnalyticsWeeklyUsers(String day) {
        super("ArcAnalytics#WeeklyUsers", day);
    }
}
