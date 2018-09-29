'use strict';

const {ArcAnalyticsDailyResult} = require('./arc-analytics-daily-result');

/**
 * A class representing Daily Sessions type.
 */
class ArcAnalyticsDailySessions extends ArcAnalyticsDailyResult {
  constructor(result) {
    super('ArcAnalytics#DailySessions', result);
  }
}

module.exports.ArcAnalyticsDailySessions = ArcAnalyticsDailySessions;
