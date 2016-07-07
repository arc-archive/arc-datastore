package com.mulesoft.arc.arcdatastore.backend.models;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

/**
 * A model class representing single session.
 */
@Entity
public class ArcSession {

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    @Id Long id;

    /**
     * Application generated ID
     * The app ID is synchronized between user machines so it is accurate.
     */
    @ApiResourceProperty
    @Index public String appId;

    /**
     * The date of the event.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    @Index public Date date;

    /**
     * Last update time.
     * Then the user hit again then this metric will be update. It is used to determine
     * if the hit belongs to existing session.
     */
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    @Index public Date lastUpdate;
}
