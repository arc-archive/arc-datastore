'use strict';
const {ArcAnalyticsCustomRangeResult} = require(
  './arc-analytics-custom-range-result');
/**
 * A class representing monthly Sessions type.
 */
class ArcAnalyticsCustomRangeUsers extends ArcAnalyticsCustomRangeResult {
  /**
   * @param {String} startDay The start day of the date range.
   * @param {String} endDay The last day of the date range.
   * @param {Number} result The result of computation.
   */
  constructor(startDay, endDay) {
    super('ArcAnalytics#CutomRangeSessions', startDay, endDay);
  }
}

module.exports.ArcAnalyticsCustomRangeUsers = ArcAnalyticsCustomRangeUsers;
