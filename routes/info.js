'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');
const {ArcInfoMessagesResponse} = require('../models/arc-info-resopnse-model');
const Datastore = require('@google-cloud/datastore');
const datastore = Datastore();
const router = express.Router();
const oauth2 = require('../lib/oauth2');

class InfoRoute extends BaseRoute {
  constructor() {
    super();
    this.initRoute();
  }

  initRoute() {
    router.options('/(.*)', this._onGetOptions.bind(this));
    router.get('/messages/', this._onGetMessages.bind(this));
    router.post('/messages', oauth2.required, this._onPostMessages.bind(this));
  }

  _onGetOptions(req, res) {
    this._appendCors(req, res);
    res.set('Content-Type', 'plain/text');
    res.status(200).send('GET,HEAD');
  }

  _appendCors(req, res) {
    const origin = req.get('origin');
    if (origin) {
      if (origin.indexOf('http://127.0.0.1') === 0 || origin.indexOf('http://localhost') === 0) {
        res.set('access-control-allow-origin', origin);
      }
    }
    res.set('allow', 'GET,HEAD');
  }

  _onGetMessages(req, res) {
    this._appendCors(req, res);
    let params;
    try {
      params = this.readParams(req);
    } catch (e) {
      return this.sendError(res, 400, e.message);
    }
    this._query(params)
    .then((response) => this.sendObject(res, response))
    .catch((cause) => this.sendError(res, 500, cause.message));
  }

  _query(config) {
    let query = datastore.createQuery('ArcInfo', 'Messages')
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
    if (config.channel) {
      query = query.filter('channel', '=', config.channel);
    }
    return datastore.runQuery(query)
    .then((results) => {
      const entities = results[0];
      const info = results[1];
      const response = new ArcInfoMessagesResponse(entities,
        config.since, config.until);
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
   * @param {Object} req
   * @return {Object} Query configuration object. If cursor has been detected it
   * only returns cusor value (under `cursor` property). Otherwise it returns
   * `since` and `until` properties.
   */
  readParams(req) {
    const cursor = req.query.cursor;
    if (cursor) {
      return {
        cursor: cursor
      };
    }
    let since = req.query.since;
    let until = req.query.until;
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

    const result = {
      since: since,
      until: until
    };

    const platform = req.query.platform;
    if (platform && ['chrome', 'electron'].indexOf(platform) !== -1) {
      result.target = platform;
    }

    const channel = req.query.channel;
    if (channel && ['stable', 'beta', 'dev'].indexOf(channel) !== -1) {
      result.channel = channel;
    }
    return result;
  }

  _onPostMessages(req, res) {
    if (!oauth2.isAdminUser(req.user.id)) {
      this.sendError(res, 401, 'Not allowed by this user.');
      return;
    }
    this.sendError(res, 500, 'Not implemented.');
  }
}

new InfoRoute();

module.exports = router;
