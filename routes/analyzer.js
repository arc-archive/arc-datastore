'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');
const {AnalyzerDatastore} = require('../lib/analytics-datastore');
const {ComputationRecordExistsError} = require('../errors/analyzer-errors');

const router = express.Router();

class AnalyzerRoute extends BaseRoute {

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
    router.get('/:type/:scope', this._onRoute.bind(this));
  }

  _onRoute(req, res) {
    const type = req.params.type;
    const scope = req.params.scope;

    if (this.allowedTypes.indexOf(type) === -1 || this.allowedScopes.indexOf(scope) === -1) {
      return this.sendError(res, 400, 'Unknown path');
    }

    const date = req.query.date;
    try {
      this.validateDate(type, date);
    } catch (e) {
      return this.sendError(res, 400, e.message);
    }

    this.runService(res, type, scope);
  }

  runService(res, type, scope) {
    const store = new AnalyzerDatastore();
    var fn = 'analyse';
    fn += type[0].toUpperCase();
    fn += type.substr(1);
    fn += scope[0].toUpperCase();
    fn += scope.substr(1);

    store[fn](this.startTime, this.endTime)
    .then(() => {
      res.status(204).end();
    })
    .catch((e) => {
      if (e instanceof ComputationRecordExistsError) {
        res.status(205).end();
        return;
      }
      this.sendError(res, 400, e.message);
    });
  }
}

new AnalyzerRoute();

module.exports = router;
