'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');
const {AnalyticsDatastore} = require('../lib/analytics-datastore');
const router = express.Router();

class AnalyticsRoute extends BaseRoute {
  constructor() {
    super();
    this.initRoute();
  }

  get allowedTypes() {
    return ['daily', 'weekly', 'monthly'];
  }

  get allowedScopes() {
    return ['users', 'sessions'];
  }

  initRoute() {
    router.get('/', this._onIndex.bind(this));
    router.get('/query/custom/:scope', this._onQueryCustom.bind(this));
    router.get('/query/:type/:scope', this._onQuery.bind(this));
    router.get('/random', this._onRandom.bind(this));
    router.post('/record', this._onRecord.bind(this));
  }

  _onIndex(req, res) {
    res.redirect('/analytics.html');
  }

  _onQueryCustom(req, res) {
    const scope = req.params.scope;
    if (this.allowedScopes.indexOf(scope) === -1) {
      return this.sendError(res, 400, 'Unknown path');
    }

    const start = req.query.start;
    const end = req.query.end;

    try {
      const startDate = this.getDatePast(start);
      const endDate = this.getDatePast(end);

      const store = new AnalyticsDatastore();
      let fn = 'queryCustomRange';
      fn += scope[0].toUpperCase();
      fn += scope.substr(1);
      console.log(fn);
      store[fn](startDate.getTime(), endDate.getTime())
      .then((result) => {
        if (!result) {
          // not yet ready
          this.sendError(res, 404, 'Not yet computed.');
        } else {
          this.sendObject(res, result);
        }
      })
      .catch((e) => {
        console.error(e);
        this.sendError(res, 400, e.message);
      });
    } catch (e) {
      console.error(e);
      return this.sendError(res, 400, e.message);
    }
  }

  _onQuery(req, res) {
    const type = req.params.type;
    const scope = req.params.scope;

    if (this.allowedTypes.indexOf(type) === -1 ||
      this.allowedScopes.indexOf(scope) === -1) {
      return this.sendError(res, 400, 'Unknown path');
    }

    const date = req.query.date;
    try {
      this.validateDate(type, date);
    } catch (e) {
      console.error(e);
      return this.sendError(res, 400, e.message);
    }

    this._runQueryService(res, type, scope);
  }

  _runQueryService(res, type, scope) {
    let fn = 'query';
    fn += type[0].toUpperCase();
    fn += type.substr(1);
    fn += scope[0].toUpperCase();
    fn += scope.substr(1);

    const store = new AnalyticsDatastore();
    store[fn](this.startTime, this.endTime)
    .then((result) => {
      if (!result) {
        // not yet ready
        this.sendError(res, 404, 'Not yet computed.');
      } else {
        this.sendObject(res, result);
      }
    })
    .catch((e) => {
      console.error(e);
      this.sendError(res, 400, e.message);
    });
  }

  _onRandom(req, res) {
    // const store = new AnalyticsDatastore();
    // store.generateData()
    // .then((result) => {
    //   if (result instanceof Error) {
    //     return this.sendError(res, 500, result.message);
    //   }
    //   res.status(204).end();
    // })
    // .catch((e) => {
    //   this.sendError(res, 500, e.message);
    // });
    this.sendError(res, 404, 'Not allowed!');
  }

  _onRecord(req, res) {
    const tz = req.body.tz;
    const anonymousId = req.body.aid;

    let message = '';
    if (!anonymousId) {
      message += 'The `aid` (anonymousId) parameter is required. ';
    }
    if (!tz) {
      message += 'The `tz` (timeZoneOffset) parameter is required.';
    }
    if (message) {
      return this.sendError(res, 400, message);
    }
    const timeZoneOffset = Number(tz);
    if (timeZoneOffset !== timeZoneOffset) {
      return this.sendError(res, 400,
        `timeZoneOffset is invalid: ${tz}. Expecting integer.`);
    }
    const store = new AnalyticsDatastore();
    store.recordSession(anonymousId, timeZoneOffset)
    .then((result) => {
      if (result instanceof Error) {
        return this.sendError(res, 500, result.message);
      }
      if (result) {
        res.status(204).end();
      } else {
        res.status(205).end();
      }
    })
    .catch((e) => {
      console.error(e);
      this.sendError(res, 500, e.message);
    });
  }
}

new AnalyticsRoute();

module.exports = router;
