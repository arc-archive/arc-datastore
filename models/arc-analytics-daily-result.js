'use strict';

/**
 * A class representing result for daily data query.
 *
 * This class should be extended by class that returns specific results.
 */
class ArcAnalyticsDailyResult {

  constructor(kind, result) {
    /**
     * The default kind value. Should be overwritten by extending class.
     *
     * @type {String}
     */
    this.kind = kind || 'ArcAnalytics#UnknownResult';
    /**
     * Result of computation.
     *
     * @type {Number}
     */
    this.result = result || 0;
  }
}

module.exports.ArcAnalyticsDailyResult = ArcAnalyticsDailyResult;
