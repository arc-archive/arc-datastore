'use strict';
const {ErrorResponse} = require('../models/error-response');

class BaseRoute {

  /**
   * Sends an error response to the client.
   *
   * @param {Object} resp Express response object.
   * @param {Number} code Error status code. Default 400.
   * @param {String} message A reason message. Default empty string.
   */
  sendError(resp, code, message) {
    const error = new ErrorResponse(code, message);
    const body = JSON.stringify(error, null, 2);
    resp.set('Content-Type', 'application/json');
    resp.status(code || 400).send(body);
  }
  /**
   * Send an API success response.
   * @param {Object} resp Express response object.
   * @param {Object} obj An object to stringnify and send.
   * @param {Number} statusCode Response status code. Default 200.
   */
  sendObject(resp, obj, statusCode) {
    statusCode = statusCode || 200;
    obj = obj || {};
    const body = JSON.stringify(obj, null, 2);
    resp.set('Content-Type', 'application/json');
    resp.status(statusCode).send(body);
  }
  /**
   * Validates date format and range for the given type.
   *
   * Daily type needs to be a date in the past (before today).
   * Weekly type needs to be date + 7 days in the past. Also it will adjust date to last Monday
   * (first day of week) if the date is not pointing to Monday.
   * Monthly have to be date adjusted to first day of month + last day of month in the past.
   *
   * This function will set `startTime` and `endTime` fields of this class if successful.
   *
   * @param {String} type Either daily, weekly or monthly.
   * @param {String} date The query start date
   * @throws TypeError On the date validation error.
   */
  validateDate(type, date) {
    this.startTime = undefined;
    this.endTime = undefined;

    if (!date) {
      throw new TypeError('The date parameter is required for this method.');
    }

    var time = Date.parse(date);
    if (time !== time) {
      let error = 'The date parameter has invalid format. Accepted format is "YYYY-MM-dd".';
      throw new TypeError(error);
    }
    // Today minimum date to check if start date is in future.
    var today = new Date();
    today.setHours(0);
    today.setMinutes(0);
    today.setSeconds(0);
    today.setMilliseconds(0);

    // Start day's minimum
    var startCalendar = new Date(time);

    const offset = startCalendar.getTimezoneOffset();
    if (offset !== 0) {
      time += (offset * 60 * 1000);
      startCalendar = new Date(time);
    }
    startCalendar.setHours(0);
    startCalendar.setMinutes(0);
    startCalendar.setSeconds(0);
    startCalendar.setMilliseconds(0);

    if (today.getTime() <= startCalendar.getTime()) {
      throw new TypeError('The date parameter must be before today.');
    }

    var endCalendar;
    if (type === 'daily') {
      endCalendar = new Date(startCalendar.getTime());
    } else if (type === 'weekly') {
      // set previous monday if current date is not a Monday
      let day = startCalendar.getDay();
      let firstDayOfWeek = 1;
      while (day !== firstDayOfWeek) {
        startCalendar.setTime(startCalendar.getTime() - 86400000); // subtract day
        day = startCalendar.getDay();
      }
      endCalendar = new Date(startCalendar.getTime());
      endCalendar.setTime(endCalendar.getTime() + 518400000); //6 * 86400000 - add 6 days
    } else if (type === 'monthly') {
      startCalendar.setDate(1); // first day of month
      endCalendar = new Date(startCalendar.getTime());
      endCalendar.setMonth(endCalendar.getMonth() + 1);
      endCalendar.setTime(endCalendar.getTime() - 86400000); //day earlier is the last day of month.
    }

    endCalendar.setDate(endCalendar.getDate() + 1); // midnight next day
    // substract one millisecond to have last millisecond of the last daty of date range
    endCalendar.setMilliseconds(-1);
    if (today.getTime() <= endCalendar.getTime()) {
      let message = 'The date end range must be before today. Date range ends ';
      message += endCalendar.getFullYear() + '-';
      message += (endCalendar.getMonth() + 1) + '-';
      message += endCalendar.getDate();
      throw new TypeError(message);
    }

    this.startTime = startCalendar.getTime();
    this.endTime = endCalendar.getTime();
  }

  getDatePast(date) {
    if (!date) {
      throw new TypeError('Invalid parameter.');
    }

    var time = Date.parse(date);
    if (time !== time) {
      let error = 'The date parameter has invalid format. Accepted format is "YYYY-MM-dd".';
      throw new TypeError(error);
    }
    // Today minimum date to check if start date is in future.
    var today = new Date();
    today.setHours(0);
    today.setMinutes(0);
    today.setSeconds(0);
    today.setMilliseconds(0);

    // Start day's minimum
    var startCalendar = new Date(time);

    const offset = startCalendar.getTimezoneOffset();
    if (offset !== 0) {
      time += (offset * 60 * 1000);
      startCalendar = new Date(time);
    }
    startCalendar.setHours(0);
    startCalendar.setMinutes(0);
    startCalendar.setSeconds(0);
    startCalendar.setMilliseconds(0);

    if (today.getTime() <= startCalendar.getTime()) {
      throw new TypeError('The date parameter must be before today.');
    }

    return startCalendar;
  }
}

module.exports.BaseRoute = BaseRoute;
