package com.mulesoft.arc.arcdatastore.backend.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This object is returned in weekly and monthly analytics data.
 * It is a representation of number of users or sessions for each day for the given time period.
 */

public class ArcAnalyticsDailyItemResult implements Comparable<ArcAnalyticsDailyItemResult> {
    /**
     * A date for the value. Represented as yyyy-MM-dd
     */
    public String day;
    /**
     * Number of session or users for given day.
     */
    public int value;

    @Override
    public int compareTo(ArcAnalyticsDailyItemResult other) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        Date thisDate;
        Date otherDate;
        try {
            thisDate = df.parse(this.day);
        } catch (Exception e) {
            thisDate = null;
        }
        try {
            otherDate = df.parse(other.day);
        } catch (Exception e) {
            otherDate = null;
        }
        if (thisDate == null && otherDate == null) {
            return 0;
        }
        if (thisDate == null) {
            return -1;
        }
        if (otherDate == null) {
            return 1;
        }
        return thisDate.compareTo(otherDate);
    }
}
