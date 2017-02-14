'use strict';

const {ArcAnalyticsDailyResult} = require('./arc-analytics-daily-result');

/**
 * A class representing Daily Users type.
 */
class ArcAnalyticsDailyUsers extends ArcAnalyticsDailyResult {

  constructor(result) {
    super('ArcAnalytics#DailyUsers', result);
  }
}

module.exports.ArcAnalyticsDailyUsers = ArcAnalyticsDailyUsers;
