'use strict';

const {ArcAnalyticsRangeResult} = require('./arc-analytics-range-result');

/**
 * A class representing weekly Users type.
 */
class ArcAnalyticsWeeklyUsers extends ArcAnalyticsRangeResult {
  /**
   * @param {String} startDay The start day of the date range.
   * @param {String} endDay The last day of the date range.
   * @param {Number} result The result of computation.
   */
  constructor(startDay, endDay, result) {
    super('ArcAnalytics#WeeklyUsers', startDay, endDay, result);
  }
}

module.exports.ArcAnalyticsWeeklyUsers = ArcAnalyticsWeeklyUsers;
