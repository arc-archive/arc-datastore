'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');
const router = express.Router();

class InfoRoute extends BaseRoute {
  constructor() {
    super();
    this.initRoute();
  }

  initRoute() {
    router.get('/health', this._onGetHealth.bind(this));
  }

  _onGetHealth(req, res) {
    res.set('Content-Type', 'plain/text');
    res.status(200).send('OK');
  }
}

new InfoRoute();

module.exports = router;
