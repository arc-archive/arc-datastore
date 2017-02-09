package com.mulesoft.arc.arcdatastore.backend;

import com.google.gson.Gson;
import com.mulesoft.arc.arcdatastore.backend.models.ErrorResponse;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * A base class for all servlets in the API.
 *
 * It contains a set of utility methods used by all child classes.
 */
public class BaseServlet extends HttpServlet {
    /**
     * Start time to be passed into the datastore query
     */
    Long startTime;
    /**
     * End time to be passed into the datastore query
     */
    Long endTime;

    /**
     * Report an API error.
     *
     * All error responses are the same: code as a status code and message associated wit the error.
     *
     * @param resp The response object.
     * @param statusCode A error status code to send. Basically it can be any valid status code but the method is meant to support errors.
     * @param message Message associated with the error.
     * @throws IOException
     */
    void reportError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        ErrorResponse r = new ErrorResponse();
        r.code = statusCode;
        r.message = message;

        Gson gson = new Gson();

        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.getWriter().print(gson.toJson(r));
    }

    /**
     * Write response with type object.
     *
     * The object will be translated to JSON and as such will be transported back to client.
     * @param resp The Servlet response object.
     * @param status Status code to set.
     * @param type Type to translate to JSON.
     * @throws IOException
     */
    void writeSuccess(HttpServletResponse resp, int status, Object type) throws IOException {
        Gson gson = new Gson();

        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.getWriter().print(gson.toJson(type));
    }

    void writeEmptySuccess(HttpServletResponse resp, int status) throws IOException {
        resp.setStatus(status);
    }

    /**
     * Validates date format and range for the given type.
     *
     * Daily type needs to be a date in the past (before today).
     * Weekly type needs to be date + 7 days in the past. Also it will adjust date to last Monday
     * (first day of week) if the date is not pointing to Monday.
     * Monthly have to be date adjusted to first day of month + last day of month in the past.
     *
     * @param type Either daily, weekly or monthly.
     * @param date The query start date
     * @throws IllegalArgumentException
     */
    void validateDate(String type, String date) throws IllegalArgumentException {
        if (date == null) {
            throw new IllegalArgumentException("The date parameter is required for this method.");
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        Date d;
        try {
            d = df.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("The date parameter has invalid format. Accepted format is \"YYYY-MM-dd\".");
        }

        // Today minimum date to check if start date is in future.
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, today.getActualMinimum(Calendar.HOUR_OF_DAY));
        today.set(Calendar.MINUTE, today.getActualMinimum(Calendar.MINUTE));
        today.set(Calendar.SECOND, today.getActualMinimum(Calendar.SECOND));
        today.set(Calendar.MILLISECOND, today.getActualMinimum(Calendar.MILLISECOND));

        // Start day minimum
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(d);
        startCalendar.set(Calendar.HOUR_OF_DAY, startCalendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        startCalendar.set(Calendar.MINUTE, startCalendar.getActualMinimum(Calendar.MINUTE));
        startCalendar.set(Calendar.SECOND, startCalendar.getActualMinimum(Calendar.SECOND));
        startCalendar.set(Calendar.MILLISECOND, startCalendar.getActualMinimum(Calendar.MILLISECOND));

        if (today.before(startCalendar) || today.equals(startCalendar)) {
            throw new IllegalArgumentException("The date parameter must be before today.");
        }
        Calendar endCalendar;

        if ("daily".equals(type)) {
            endCalendar = (Calendar) startCalendar.clone();
        } else if ("weekly".equals(type)) {
            // set previous monday if current date is not a Monday
            int day = startCalendar.get(Calendar.DAY_OF_WEEK);
            int firstDayOfWeek = startCalendar.getFirstDayOfWeek();
            while (day != firstDayOfWeek) {
                startCalendar.add(Calendar.DATE, -1);
                day = startCalendar.get(Calendar.DAY_OF_WEEK);
            }

            endCalendar = (Calendar) startCalendar.clone();
            endCalendar.add(Calendar.DAY_OF_MONTH, 6);
        } else {
            startCalendar.set(Calendar.DATE, today.getActualMinimum(Calendar.DATE));
            endCalendar = (Calendar) startCalendar.clone();
            endCalendar.set(Calendar.DATE, endCalendar.getActualMaximum(Calendar.DATE));
        }

        endCalendar.set(Calendar.HOUR_OF_DAY, endCalendar.getActualMaximum(Calendar.HOUR_OF_DAY));
        endCalendar.set(Calendar.MINUTE, endCalendar.getActualMaximum(Calendar.MINUTE));
        endCalendar.set(Calendar.SECOND, endCalendar.getActualMaximum(Calendar.SECOND));
        endCalendar.set(Calendar.MILLISECOND, endCalendar.getActualMaximum(Calendar.MILLISECOND));

        // At this point the end date is set to last day of week or month depending on type.

        if (today.before(endCalendar) || today.equals(endCalendar)) {
            throw new IllegalArgumentException("The date end range must be before today. Date range ends " + df.format(endCalendar.getTime()));
        }

        startTime = startCalendar.getTimeInMillis();
        endTime = endCalendar.getTimeInMillis();
    }
}
