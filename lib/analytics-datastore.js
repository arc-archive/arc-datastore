'use strict';

// By default, the client will authenticate using the service account file
// specified by the GOOGLE_APPLICATION_CREDENTIALS environment variable and use
// the project specified by the GCLOUD_PROJECT environment variable.
// These environment variables are set automatically on Google App Engine
const Datastore = require('@google-cloud/datastore');
const dateFormat = require('./date-format');
const {ComputationRecordExistsError} = require('../errors/analyzer-errors');
const {AnalyticsUserQuery, AnalyticsSessionQuery} = require('./analytics-computation-query');
const {ArcAnalyticsDailyItemResult}  = require('../models/arc-analytics-daily-item-result');
const {ArcAnalyticsDailyUsers} = require('../models/arc-analytics-daily-users');
const {ArcAnalyticsWeeklyUsers} = require('../models/arc-analytics-weekly-users');
const {ArcAnalyticsMonthlyUsers} = require('../models/arc-analytics-monthly-users');
const {ArcAnalyticsDailySessions} = require('../models/arc-analytics-daily-sessions');
const {ArcAnalyticsWeeklySessions} = require('../models/arc-analytics-weekly-sessions');
const {ArcAnalyticsMonthlySessions} = require('../models/arc-analytics-monthly-sessions');
const uuidV4 = require('uuid/v4');

// Instantiate a datastore client
const datastore = Datastore();

class AnalyticsDatastoreBase {
  /**
   * Datastore namespace name.
   */
  get namespace() {
    return 'analytics';
  }
}

class AnalyticsDatastore extends AnalyticsDatastoreBase {
  /**
   * Records user and user's session in analytics datastore.
   *
   * @param {String} applicationId The client generated Application ID.
   * @param {Number} timeZoneOffset Client's timezone offset.
   * @return True if new session has been recorded or false if existing session has been updated.
   */
  recordSession(applicationId, timeZoneOffset) {
    var time = Date.now();
    time += timeZoneOffset;

    return this.ensureUserRecord(applicationId, time)
    .then(() => this.ensureSession(applicationId, time))
    .catch((e) => {
      console.log('recordSession error', e.message);
      console.log(e.stack);
      throw e;
    });
  }
  /**
   * Ensures that the record in User entity group exists for given application ID.
   *
   * @param {String} applicationId Anonymized application ID.
   * @param {Number} time A timestamp of the day of the user visit.
   * @return {Promise<Object>} Promise will resolve when there's a user object or if one hyas been
   * created.
   */
  ensureUserRecord(applicationId, time) {
    return this.getActiveUser(applicationId, time)
    .then((entity) => {
      if (!entity) {
        return this.createActiveUser(applicationId);
      }
    });
  }
  /**
   * Gets existing record for the application ID.
   * It search for application id that has been recorded today.
   *
   * @param {String} applicationId Anonymized application ID.
   * @param {Number} time A timestamp of the day of the user visit.
   * @return {Promise<Object>} Promise Will resolve to entity object or to null if not found.
   */
  getActiveUser(applicationId, time) {
    const entryKey = this.getUserKey(applicationId, time);
    return datastore.get(entryKey)
    .then((data) => {
      return data[0];
    })
    .catch(() => {
      return null;
    });
  }
  /**
   * Creates a user record for today.
   *
   * @param {String} applicationId Anonymized application ID.
   * @param {Number} time A timestamp of the day of the user visit.
   * @return {Promise<}
   */
  createActiveUser(applicationId, time) {
    const entryKey = this.getUserKey(applicationId, time);
    return datastore.upsert({
      key: entryKey,
      data: {
        appId: applicationId,
        day: Date.now()
      }
    });
  }
  /**
   * Generates a User group key based on the application ID.
   *
   * @param {String} applicationId Anonymized application ID.
   * @param {Number} time A timestamp of the day of the user visit.
   */
  getUserKey(applicationId, time) {
    time = time || Date.now();
    const entryStringKey = dateFormat(new Date(time), 'YYYYMMdd');
    return datastore.key({
      namespace: this.namespace,
      path: ['User', applicationId + '/' + entryStringKey]
    });
  }
  /**
   * Ensures that the user session exists in the datastore.
   *
   * @param {String} applicationId Anonymized application ID.
   * @param {Number} time A timestamp of the day of the user visit.
   * @return {Promise<boolean>} If true then new session wass created and false if session already
   * existed in the datastore.
   */
  ensureSession(applicationId, time) {
    return this.getActiveSession(applicationId, time)
    .then((entity) => {
      if (entity) {
        return this.updateActiveSession(entity, time);
      }
      return this.createActiveSession(applicationId, time);
    });
  }
  /**
   * Gets a user session recorded in last 30 minutes.
   *
   * @param {String} applicationId Anonymized application ID.
   * @param {Number} time A timestamp of the day of the user visit.
   * @return {Promise<Object>} Promise resolved to an entity or to null if session not found.
   */
  getActiveSession(applicationId, time) {
    var past = time - 1800000;
    let query = datastore.createQuery('analytics', 'Session')
      .filter('appId', '=', applicationId)
      .filter('lastActive', '>=', past)
      .order('lastActive', {
        descending: true
      })
      .limit(1);
    return datastore.runQuery(query)
    .then((results) => {
      const entities = results[0];
      if (entities && entities.length) {
        return entities[0];
      }
      return null;
    });
  }

  updateActiveSession(entity, time) {
    entity.lastActive = time;
    return datastore.upsert(entity)
    .then(() => {
      return false;
    });
  }

  createActiveSession(applicationId, time) {
    const entryKey = datastore.key({
      namespace: this.namespace,
      path: ['Session']
    });
    var entity = {
      key: entryKey,
      data: {
        appId: applicationId,
        day: time,
        lastActive: time
      }
    };
    return datastore.save(entity)
    .then(() => {
      return true;
    });
  }
  /**
   * Random data generation.
   *
   * Be sure to hash body of this function when deploying to app engine!
   */
  generateData() {
    var size = 270;
    var list = [];
    var appids = [];

    for (let i = 0; i < size; i++) {
      let time = this._getRandomDay();
      let appId = uuidV4();
      appids.push(appId);
      let key = datastore.key({
        namespace: this.namespace,
        path: ['Session']
      });
      var entity = {
        key: key,
        data: {
          appId: appId,
          day: time,
          lastActive: time
        }
      };
      list.push(entity);
    }

    size = Math.round(size * 0.7);
    for (let i = 0; i < size; i++) {
      let time = this._getRandomDay();
      let appId = appids[i];
      let key = this.getUserKey(appId, time);
      let entity = {
        key: key,
        data: {
          appId: appId,
          day: time
        }
      };
      list.push(entity);
    }

    return datastore.save(list)
    .then(() => {
      return true;
    });
  }

  _getRandomDay() {
    var d = new Date();
    d.setMonth(d.getMonth() - 1);
    d.setDate(1);

    var _d = new Date(d.getTime());
    _d.setMonth(_d.getMonth() + 1);
    _d.setDate(0);

    let random = this._randBetween(0, _d.getDate());
    if (random > 0) {
      d.setTime(d.getTime() + random * 86400000);
    }
    return d.getTime();
  }

  _randBetween(start, end) {
    return start + Math.round(Math.random() * (end - start));
  }

  /**
   * Gets the computed number of users for given day
   *
   * @param {Number} time A timestamp of the day
   */
  queryDailyUsers(time) {
    let key = datastore.key({
      namespace: this.namespace,
      path: ['DailyUsers', dateFormat(new Date(time), 'yyyy-MM-dd')]
    });
    return datastore.get(key)
    .then((entities) => {
      let entity = entities[0];
      if (!entity) {
        return null;
      }

      const result = new ArcAnalyticsDailyUsers(entity.users);
      return result;
    });
  }

  /**
   * Gets the computed number of users for given week bound in given date range
   * The function uses the `start` argument to get the data from the WeeklyUsers group
   * and `start` and `end` to query for days.
   *
   * @param {Number} start A timestamp of the start day in the date range.
   * @param {Number} start A timestamp of the last day in the date range.
   */
  queryWeeklyUsers(start, end) {
    const day = dateFormat(new Date(start), 'yyyy-MM-dd');
    const lastDay = dateFormat(new Date(end), 'yyyy-MM-dd');
    let key = datastore.key({
      namespace: this.namespace,
      path: ['WeeklyUsers', day]
    });
    return datastore.get(key)
    .then((entities) => {
      let entity = entities[0];
      if (!entity) {
        return null;
      }
      const result = new ArcAnalyticsWeeklyUsers(day, lastDay, entity.users);
      return this._queryDailyUsers(start, end)
      .then((items) => {
        result.items = items;
        return result;
      });
    });
  }

  /**
   * Gets the computed number of users for given week bound in given date range
   * The function uses the `start` argument to get the data from the `MonthlyUsers` group
   * and `start` and `end` to query for days.
   *
   * @param {Number} start A timestamp of the start day in the date range.
   * @param {Number} start A timestamp of the last day in the date range.
   */
  queryMonthlyUsers(start, end) {
    const day = dateFormat(new Date(start), 'yyyy-MM');
    const lastDay = dateFormat(new Date(end), 'yyyy-MM');
    let key = datastore.key({
      namespace: this.namespace,
      path: ['MonthlyUsers', day]
    });
    return datastore.get(key)
    .then((entities) => {
      let entity = entities[0];
      if (!entity) {
        return null;
      }
      const result = new ArcAnalyticsMonthlyUsers(day, lastDay, entity.users);
      return this._queryDailyUsers(start, end)
      .then((items) => {
        result.items = items;
        return result;
      });
    });
  }

  _queryDailyUsers(start, end) {
    let query = datastore.createQuery('analytics', 'DailyUsers')
      .filter('day', '>=', start)
      .filter('day', '<=', end);
    return datastore.runQuery(query)
    .then((results) => {
      const entities = results[0];
      if (!entities || !entities.length) {
        return [];
      }
      let keySymbol = datastore.KEY;
      return entities.map((entity) => {
        let name = entity[keySymbol].name;
        return new ArcAnalyticsDailyItemResult(name, entity.users);
      });
    });
  }

  /**
   * Gets the computed number of sessions for given day
   *
   * @param {Number} time A timestamp of the day
   */
  queryDailySessions(time) {
    let key = datastore.key({
      namespace: this.namespace,
      path: ['DailySessions', dateFormat(new Date(time), 'yyyy-MM-dd')]
    });
    return datastore.get(key)
    .then((entities) => {
      let entity = entities[0];
      if (!entity) {
        return null;
      }

      const result = new ArcAnalyticsDailySessions(entity.sessions);
      return result;
    });
  }

  /**
   * Gets the computed number of users for given week bound in given date range
   * The function uses the `start` argument to get the data from the WeeklyUsers group
   * and `start` and `end` to query for days.
   *
   * @param {Number} start A timestamp of the start day in the date range.
   * @param {Number} start A timestamp of the last day in the date range.
   */
  queryWeeklySessions(start, end) {
    const day = dateFormat(new Date(start), 'yyyy-MM-dd');
    const lastDay = dateFormat(new Date(end), 'yyyy-MM-dd');
    let key = datastore.key({
      namespace: this.namespace,
      path: ['WeeklySessions', day]
    });
    return datastore.get(key)
    .then((entities) => {
      let entity = entities[0];
      if (!entity) {
        return null;
      }
      const result = new ArcAnalyticsWeeklySessions(day, lastDay, entity.sessions);
      return this._queryDailySessions(start, end)
      .then((items) => {
        result.items = items;
        return result;
      });
    });
  }

  /**
   * Gets the computed number of sessions for given month.
   * The function uses the `start` argument to get the data from the `MonthlySessions` group
   * and `start` and `end` to query for days.
   *
   * @param {Number} start A timestamp of the start day in the date range.
   * @param {Number} start A timestamp of the last day in the date range.
   */
  queryMonthlySessions(start, end) {
    const day = dateFormat(new Date(start), 'yyyy-MM');
    const lastDay = dateFormat(new Date(end), 'yyyy-MM');
    let key = datastore.key({
      namespace: this.namespace,
      path: ['MonthlySessions', day]
    });
    return datastore.get(key)
    .then((entities) => {
      let entity = entities[0];
      if (!entity) {
        return null;
      }
      const result = new ArcAnalyticsMonthlySessions(day, lastDay, entity.sessions);
      return this._queryDailySessions(start, end)
      .then((items) => {
        result.items = items;
        return result;
      });
    });
  }

  _queryDailySessions(start, end) {
    let query = datastore.createQuery('analytics', 'DailySessions')
      .filter('day', '>=', start)
      .filter('day', '<=', end);
    return datastore.runQuery(query)
    .then((results) => {
      const entities = results[0];
      if (!entities || !entities.length) {
        return [];
      }
      let keySymbol = datastore.KEY;
      return entities.map((entity) => {
        let name = entity[keySymbol].name;
        return new ArcAnalyticsDailyItemResult(name, entity.sessions);
      });
    });
  }
}

class AnalyzerDatastore extends AnalyticsDatastoreBase {
  /**
   * Computes the data for daily sessions.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  analyseDailySessions(start, end) {
    return this.computeSessions(start, end, 'yyyy-MM-dd', 'DailySessions');
  }
  /**
   * Computes the data for weekly sessions.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  analyseWeeklySessions(start, end) {
    return this.computeSessions(start, end, 'yyyy-MM-dd', 'WeeklySessions');
  }
  /**
   * Computes the data for monthly sessions.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  analyseMonthlySessions(start, end) {
    return this.computeSessions(start, end, 'yyyy-MM', 'MonthlySessions');
  }

  /**
   * Performs a query on Sessions store.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @param keyFormat Database key format as a Date format
   * @param groupName The storage name where to save the result.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  computeSessions(start, end, keyFormat, groupName) {
    const entryStringKey = dateFormat(new Date(start), keyFormat);
    const entryKey = datastore.key({
      namespace: this.namespace,
      path: [groupName, entryStringKey]
    });

    // console.log('Performing a gquery to the sessions stroere.');
    // console.log('Time range: ', new Date(start), new Date(end));
    // console.log('Results will be saved into', groupName);

    return datastore.get(entryKey)
    .then((result) => {
      if (result[0]) {
        throw new ComputationRecordExistsError();
      }
      const process = new AnalyticsSessionQuery(start, end);
      return process.query();
    })
    .then((sessions) => {
      return datastore.save({
        key: entryKey,
        data: {
          day: start,
          sessions: sessions
        }
      });
    });
  }

  /**
   * Computes the data for daily users.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  analyseDailyUsers(start, end) {
    return this.computeUsers(start, end, 'yyyy-MM-dd', 'DailyUsers');
  }
  /**
   * Computes the data for weekly users.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  analyseWeeklyUsers(start, end) {
    return this.computeUsers(start, end, 'yyyy-MM-dd', 'WeeklyUsers');
  }
  /**
   * Computes the data for monthly users.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  analyseMonthlyUsers(start, end) {
    return this.computeUsers(start, end, 'yyyy-MM', 'MonthlyUsers');
  }
  /**
   * Performs a query on Users store.
   *
   * @param {Number} start A long representing a midnight for the computation day.
   * @param {Number} end A long representing a millisecond before midnight for the last
   * computation day.
   * @param keyFormat Database key format as a Date format
   * @param groupName The storage name where to save the result.
   * @return {Promise} Promise will resolve if the data was computed and result saved to the
   * database.
   * @throws ComputationRecordExistsError
   */
  computeUsers(start, end, keyFormat, groupName) {
    const entryStringKey = dateFormat(new Date(start), keyFormat);
    const entryKey = datastore.key({
      namespace: this.namespace,
      path: [groupName, entryStringKey]
    });

    return datastore.get(entryKey)
    .then((result) => {
      if (result[0]) {
        throw new ComputationRecordExistsError();
      }
      const process = new AnalyticsUserQuery(start, end);
      return process.query();
    })
    .then((users) => {
      return datastore.save({
        key: entryKey,
        data: {
          day: start,
          users: users
        }
      });
    });
  }
}

module.exports.AnalyticsDatastore = AnalyticsDatastore;
module.exports.AnalyzerDatastore = AnalyzerDatastore;
