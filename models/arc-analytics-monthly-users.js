'use strict';

const {ArcAnalyticsRangeResult} = require('./arc-analytics-range-result');

/**
 * A class representing monthly Users type.
 */
class ArcAnalyticsMonthlyUsers extends ArcAnalyticsRangeResult {
  /**
   * @param {String} startDay The start day of the date range.
   * @param {String} endDay The last day of the date range.
   * @param {Number} result The result of computation.
   */
  constructor(startDay, endDay, result) {
    super('ArcAnalytics#MonthlyUsers', startDay, endDay, result);
  }
}

module.exports.ArcAnalyticsMonthlyUsers = ArcAnalyticsMonthlyUsers;
