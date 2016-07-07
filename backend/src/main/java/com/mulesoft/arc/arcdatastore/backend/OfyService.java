package com.mulesoft.arc.arcdatastore.backend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.mulesoft.arc.arcdatastore.backend.models.ArcSession;

/**
 * Objectify service.
 */
public class OfyService {

    static {
        ObjectifyService.register(ArcSession.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }

}
