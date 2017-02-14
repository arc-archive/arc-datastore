'use strict';

const {ArcAnalyticsRangeResult} = require('./arc-analytics-range-result');

/**
 * A class representing monthly Sessions type.
 */
class ArcAnalyticsMonthlySessions extends ArcAnalyticsRangeResult {
  /**
   * @param {String} startDay The start day of the date range.
   * @param {String} endDay The last day of the date range.
   * @param {Number} result The result of computation.
   */
  constructor(startDay, endDay, result) {
    super('ArcAnalytics#MonthlySessions', startDay, endDay, result);
  }
}

module.exports.ArcAnalyticsMonthlySessions = ArcAnalyticsMonthlySessions;
