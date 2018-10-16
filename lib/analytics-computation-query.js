'use strict';
const Datastore = require('@google-cloud/datastore');

class AnalyticsComputationQuery {
  /**
   * Error will be thrown if any of the arguments missing.
   *
   * @param {Number} fromDate Query filter start date
   * @param {Number} toDate Query filter end date.
   * @param {String} entityGroup Name of the entity group to query for data.
   */
  constructor(fromDate, toDate, entityGroup) {
    if (!fromDate || !toDate) {
      throw new TypeError('From and To dates are required parameters.');
    }
    if (!entityGroup) {
      throw new TypeError('The required argument entityGroup is not defined.');
    }
    /**
     * Google cloud datastore.
     *
     * @type {Object}
     */
    this.datastore = Datastore();
    /**
     * Entity group to query.
     */
    this.entityGroup = entityGroup;
    /**
     * Query filter start time.
     *
     * @type {Number}
     */
    this.fromDate = fromDate;
    /**
     * Query filter end time.
     *
     * @type {Number}
     */
    this.toDate = toDate;
  }

  /**
   * Performs the query to the datastore and computes results.
   *
   * @return {Promise<Number>} Resolved promise will return number of users /
   * sessions for given time period.
   */
  query() {
    return this._makeQuery()
    .then((result) => this._computeResult(result));
  }

  /**
   * The same as the `query()` function but recursively performs the query.
   *
   * @param {String} pageCursor a cursor returned by previous query.
   * @param {?Array} arr
   * @return {Promise}
   */
  _makeQuery(pageCursor, arr) {
    if (!arr) {
      arr = [];
    }
    let query = this.datastore.createQuery('analytics', this.entityGroup)
      .select('__key__')
      .filter('day', '>=', this.fromDate)
      .filter('day', '<=', this.toDate)
      .limit(10000);

    if (pageCursor) {
      query = query.start(pageCursor);
    }
    return this.datastore.runQuery(query)
    .then((results) => {
      const entities = results[0];
      arr = arr.concat(results[0]);
      const info = results[1];
      if (info.moreResults === Datastore.NO_MORE_RESULTS || !entities.length) {
        return arr;
      }
      return this._makeQuery(info.endCursor, arr);
    });
  }
  /**
   * Function to be implemented by extending classes to compute number of
   * sessions / users based on the database query results.
   *
   * @param {Array<Entity>} entities Data read from the datastore.
   */
  _computeResult() {
    throw new TypeError(
      'The `_computeResult` method not implemented in query class.');
  }
}

/**
 * A class that is responsible for making a query to the users analytics data.
 */
class AnalyticsUserQuery extends AnalyticsComputationQuery {
  /**
   * Error will be thrown if any of the arguments missing.
   *
   * @param {Number} fromDate Query filter start date
   * @param {Number} toDate Query filter end date.
   */
  constructor(fromDate, toDate) {
    super(fromDate, toDate, 'User');
  }

  _computeResult(entities) {
    // console.log('_computeResult::entities.length', entities.length);
    if (!entities || !entities.length) {
      return 0;
    }
    let users = 0;
    const keySymbol = this.datastore.KEY;
    const uuids = {};
    entities.forEach(function(entity) {
      const name = entity[keySymbol].name;
      // subtracts "/YYYYMMdd"
      const appId = name.substr(0, name.length - 9);
      if (appId in uuids) {
        return;
      }
      uuids[appId] = true;
      users++;
    });
    return users;
  }
}

/**
 * A class that is responsible for making a query to the users analytics data.
 */
class AnalyticsSessionQuery extends AnalyticsComputationQuery {
  /**
   * Error will be thrown if any of the arguments missing.
   *
   * @param {Number} fromDate Query filter start date
   * @param {Number} toDate Query filter end date.
   */
  constructor(fromDate, toDate) {
    super(fromDate, toDate, 'Session');
  }

  _computeResult(entities) {
    if (!entities || !entities.length) {
      return 0;
    }
    return entities.length;
  }
}

module.exports.AnalyticsUserQuery = AnalyticsUserQuery;
module.exports.AnalyticsSessionQuery = AnalyticsSessionQuery;
