package com.mulesoft.arc.arcdatastore.backend.models;

import java.util.Date;

/**
 * A response to the query result.
 */
public class UsersComputationResult {
    /**
     * Query start date
     */
    public Date startDate;
    /**
     * Query end date
     */
    public Date endDate;
    /**
     * Number of users in this time period.
     * This is a number of unique
     */
    public Integer users;
}
