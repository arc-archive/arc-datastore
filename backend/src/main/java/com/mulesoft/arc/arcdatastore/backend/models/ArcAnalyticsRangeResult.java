package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A class representing result for date range data query
 */

public abstract class ArcAnalyticsRangeResult {
    final String kind;
    /**
     * The first day (Monday) for given time period if the query was related to weekly data or
     * first day of month if to monthly data.
     *
     * It may be different than the `day` query parameter if the `day` is not the Monday or the
     * first day of month.
     */
    final String day;
    /**
     *  Number of recorded users or sessions for given time period.
     */
    public Integer result;
    /**
     * Daily results for given time period.
     */
    public ArcAnalyticsDailyItemResult[] items;

    ArcAnalyticsRangeResult(String kind, String day) {
        this.kind = kind;
        this.day = day;
    }
}
