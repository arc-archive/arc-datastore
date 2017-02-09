package com.mulesoft.arc.arcdatastore.backend;

/**
 * An exception thrown when the analytics task servlet is trying to perform a computation on a
 * set that has been already computed.
 */

class ComputationRecordExistsException extends Exception {

    ComputationRecordExistsException() { super(); }
//    public ComputationRecordExistsException(String message) { super(message); }
//    public ComputationRecordExistsException(String message, Throwable cause) { super(message, cause); }
//    public ComputationRecordExistsException(Throwable cause) { super(cause); }

}
