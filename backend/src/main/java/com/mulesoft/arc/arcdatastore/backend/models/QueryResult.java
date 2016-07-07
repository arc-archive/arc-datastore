package com.mulesoft.arc.arcdatastore.backend.models;

import com.google.api.server.spi.config.ApiResourceProperty;

import java.util.Date;

/**
 * A response to the query result.
 */
public class QueryResult {
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
    @ApiResourceProperty
    public Long sessions;
    /**
     * Number of users in this time period.
     * This is a number of unique
     */
    @ApiResourceProperty
    public Long users;
}
