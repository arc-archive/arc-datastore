package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A class representing result for daily data query
 */

public abstract class ArcAnalyticsDailyResult {
    public final String kind;
    public int result;

    ArcAnalyticsDailyResult(String kind) {
        this.kind = kind;
    }
}
