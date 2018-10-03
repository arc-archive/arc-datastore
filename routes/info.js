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
      params = this._readQueryParams(req.query);
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
   * @param {Object} query Query part of the request
   * @return {Object} Query configuration object. If cursor has been detected it
   * only returns cusor value (under `cursor` property). Otherwise it returns
   * `since` and `until` properties.
   */
  _readQueryParams(query) {
    const cursor = query.cursor;
    if (cursor) {
      return {
        cursor: cursor
      };
    }
    let since = query.since;
    let until = query.until;
    if (until === 'now') {
      until = Date.now();
    } else if (until) {
      until = Number(until);
      if (until !== until) {
        throw new Error('Invalid "until" timestamp');
      }
    }
    if (since) {
      since = Number(since);
      if (since !== since) {
        throw new Error('Invalid "since" timestamp');
      }
    }
    if (since > until) {
      throw new Error('"since" cannot be higher than until');
    }
    let limit = query.limit;
    if (limit) {
      limit = Number(limit);
      if (limit !== limit) {
        throw new Error('Invalid "limit" value');
      }
    }
    const result = {};
    if (since) {
      result.since = since;
    }
    if (until) {
      result.until = until;
    }
    if (limit) {
      result.limit = limit;
    }
    const platform = query.platform;
    if (platform) {
      result.target = platform;
    }
    const channel = query.channel;
    if (channel) {
      result.channel = channel;
    }
    return result;
  }

  _onPostMessages(req, res) {
    if (!oauth2.isAdminUser(req.user.id)) {
      this.sendError(res, 401, 'Not allowed by this user.');
      return;
    }
    let entry;
    try {
      entry = this._bodyToMessageProperties(req.body || {});
    } catch (e) {
      this.sendError(res, 400, e.message);
      return;
    }
    this.store.insert(entry)
    .then(() => {
      this.sendObject(res, {message: 'Messagee created'});
    })
    .catch((cause) => {
      this.sendError(res, 500, cause.message);
    });
  }
  /**
   * Converts message create body to datastore entry with indexes definition.
   * @param {Object} body Request body
   * @return {Array} Entry definition
   * @throws An error when any required property is missing.
   */
  _bodyToMessageProperties(body) {
    const result = [];
    const missing = [];
    const indexed = ['target', 'time', 'channel'];
    ['abstract', 'actionurl', 'cta', 'target', 'title', 'channel']
    .forEach((prop) => {
      if (!body[prop]) {
        missing[missing.length] = prop;
      } else {
        let value = body[prop];
        if (prop === 'target' && !(value instanceof Array)) {
          value = [value];
        }
        const data = {
          name: prop,
          value
        };
        if (indexed.indexOf(prop) === -1) {
          data.excludeFromIndexes = true;
        }
        result[result.length] = data;
      }
    });
    if (missing.length) {
      throw new Error(missing.join(', ') + ' is required');
    }
    // Time property
    let time = body.time;
    if (time) {
      if (isNaN(time)) {
        throw new Error('"time" is not a number');
      }
      time = Number(time);
    } else {
      time = Date.now();
    }
    result[result.length] = {
      name: 'time',
      value: time
    };
    return result;
  }
}

new InfoRoute();

module.exports = router;
module.exports.InfoRoute = InfoRoute;
