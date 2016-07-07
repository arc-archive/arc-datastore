package com.mulesoft.arc.arcdatastore.backend.models;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A model class representing single session.
 */
@Entity
public class ArcSession {

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    @Id Long id;

    @ApiResourceProperty
    @Index public String appId;

    @Index
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Date date;
}
