'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');
const router = express.Router();
const oauth2 = require('../lib/oauth2');
const {MessagesDatastore} = require('../lib/messages-datastore');

class InfoRoute extends BaseRoute {
  constructor() {
    super();
    this.initRoute();
  }

  get store() {
    if (!this.__store) {
      this.__store = new MessagesDatastore();
    }
    return this.__store;
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
    this.store.query(params)
    .then((response) => this.sendObject(res, response))
    .catch((cause) => {
      console.error(cause);
      this.sendError(res, 500, cause.message);
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
    const b = req.body;
    const missing = [];
    const entry = {};

    ['abstract', 'actionurl', 'cta', 'target', 'title', 'channel']
    .forEach((prop) => {
      if (!b[prop]) {
        missing[missing.length] = prop;
      } else {
        entry[prop] = b[prop];
      }
    });
    if (missing.length) {
      this.sendError(res, 400, missing.join(', ') + ' is required');
      return;
    }
    if (b.time) {
      if (isNaN(b.time)) {
        this.sendError(res, 400, 'time is not a number');
        return;
      }
      entry.time = Number(b.time);
    } else {
      entry.time = Date.now();
    }

    this.store.insert(entry)
    .then(() => {
      this.sendObject(res, {message: 'Messagee created'});
    })
    .catch((cause) => {
      this.sendError(res, 500, cause.message);
    });
  }
}

new InfoRoute();

module.exports = router;
