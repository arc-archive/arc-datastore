'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');
const {ArcInfoMessagesResponse} = require('../models/arc-info-resopnse-model');
const Datastore = require('@google-cloud/datastore');
const datastore = Datastore();
const router = express.Router();

class InfoRoute extends BaseRoute {

  constructor() {
    super();
    this.initRoute();
  }

  initRoute() {
    router.get('/', this._onGetMessages.bind(this));
  }

  _onGetMessages(req, res) {
    var params;
    try {
      params = this.readParams(req);
    } catch (e) {
      return this.sendError(res, 400, e.message);
    }
    this._query(params)
    .then(response => this.sendObject(res, response))
    .catch(cause => this.sendError(res, 500, cause.message));
  }

  _query(config) {
    console.log('Making a query for', config);
    var query = datastore.createQuery('ArcInfo', 'Messages')
    .order('time', {
      descending: true
    });
    if (config.cursor) {
      query = query.start(config.cursor);
    } else {
      query = query.filter('time', '<=', config.until)
        .filter('time', '>=', config.since);
    }
    if (config.target) {
      query = query.filter('target', '=', config.target);
    }
    return datastore.runQuery(query)
    .then((results) => {
      const entities = results[0];
      const info = results[1];
      const response = new ArcInfoMessagesResponse(entities);
      if (info.moreResults !== Datastore.NO_MORE_RESULTS) {
        response.cursor = info.endCursor;
      }
      return response;
    });
  }

  /**
   * Reads `since` and `until` properties from the request or creates default
   * ones (last 30 days time period).
   *
   * @return {Object} Query configuration object. If cursor has been detected it
   * only returns cusor value (under `cursor` property). Otherwise it returns
   * `since` and `until` properties.
   */
  readParams(req) {
    var cursor = req.query.cursor;
    if (cursor) {
      return {
        cursor: cursor
      };
    }
    var since = req.query.since;
    var until = req.query.until;
    if (!until || until === 'now') {
      until = Date.now();
    } else {
      until = Number(req.query.until);
      if (until !== until) {
        throw new Error('Invalid "until" timestamp');
      }
    }

    if (!since) {
      since = until - 2.592e+9;
    } else {
      since = Number(since);
      if (since !== since) {
        throw new Error('Invalid "since" timestamp');
      }
    }
    if (since > until) {
      throw new Error('"since" cannot be higher than until');
    }

    var result = {
      since: since,
      until: until
    };

    var platform = req.query.platform;
    if (platform && ['chrome', 'electron'].indexOf(platform) !== -1) {
      result.target = platform;
    }

    return result;
  }
}

new InfoRoute();

module.exports = router;
