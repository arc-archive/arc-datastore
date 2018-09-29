'use strict';

/**
 * This object is returned in weekly and monthly analytics data.
 * It is a representation of number of users or sessions for each day
 * for the given time period.
 */
class ArcAnalyticsDailyItemResult {
  constructor(day, value) {
    /**
     * A date for the value. Represented as yyyy-MM-dd
     *
     * @type {String}
     */
    this.day = day;
    /**
     * Number of session or users for given day.
     *
     * @type {Number}
     */
    this.value = value;
  }

  sort(a, b) {
    const thisDate = Date.parse(a.day);
    const otherDate = Date.parse(b.day);
    if (!thisDate && !otherDate) {
      return 0;
    }
    if (!thisDate) {
      return -1;
    }
    if (!otherDate) {
      return 1;
    }
    if (thisDate > otherDate) {
      return 1;
    }
    if (thisDate < otherDate) {
      return -1;
    }
    return 0;
  }
}

module.exports.ArcAnalyticsDailyItemResult = ArcAnalyticsDailyItemResult;
