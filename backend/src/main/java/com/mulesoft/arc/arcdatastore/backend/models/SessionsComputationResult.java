package com.mulesoft.arc.arcdatastore.backend.models;

import java.util.Date;

/**
 * An object returned by the session computation task.
 */
public class SessionsComputationResult {
    /**
     * Query start date
     */
    public Date startDate;
    /**
     * Query end date
     */
    public Date endDate;
    /**
     * Number of recorded session in this time period.
     * This is a number of records in the database.
     */
    public Integer sessions;
}
