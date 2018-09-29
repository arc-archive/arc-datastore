'use strict';
/**
 * A class representing result for daily data query.
 *
 * This class should be extended by class that returns specific results.
 */
class ArcAnalyticsRangeResult {

  constructor(kind, startDay, endDay, result) {
    /**
     * The default kind value. Should be overwritten by extending class.
     *
     * @type {String}
     */
    this.kind = kind || 'ArcAnalytics#UnknownRangeResult';
    /**
     * The first day (Monday) for given time period if the query was
     * related to weekly data or first day of month if to monthly data.
     *
     * It may be different than the `day` query parameter if the `day` is
     * not the Monday or the first day of month.
     *
     * @type {String}
     */
    this.startDay = startDay || '';
    /**
     * Last day of the date range. In format YYYY-MM-dd
     *
     * @type {String}
     */
    this.endDay = endDay || '';
    /**
     *  Number of recorded users or sessions for given time period.
     *
     * @type {Number}
     */
    this.result = result || 0;
    /**
     * Daily results list for given time period.
     *
     * @type {Array<ArcAnalyticsDailyItemResult>}
     */
    this.items = [];
  }
}

module.exports.ArcAnalyticsRangeResult = ArcAnalyticsRangeResult;
