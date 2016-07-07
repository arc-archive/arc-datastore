package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * A response to the insert request.
 */
public class InsertResult {
    /**
     * True if the insert was successful
     */
    public boolean success;
    /**
     * True when the session already existed and the actual insert wasn't performed.
     */
    public boolean continueSession;
}
