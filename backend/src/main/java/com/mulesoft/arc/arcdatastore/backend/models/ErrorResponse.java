package com.mulesoft.arc.arcdatastore.backend.models;

/**
 * Error report from the server.
 */
public class ErrorResponse {
    /**
     * Status code.
     */
    public int code;
    /**
     * Message explaining error.
     */
    public String message;

    public ErrorResponse() {}
}
