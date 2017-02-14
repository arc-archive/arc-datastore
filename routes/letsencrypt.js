'use strict';

const express = require('express');
const {BaseRoute} = require('./base-route');

const router = express.Router();

class LetsEncryptRoute extends BaseRoute {

  get challengeResponse() {
    return '._DW7L_TfipACEMVC7uKg7PeC6aIV1ON1GkYI5EvOTQ0';
  }

  constructor() {
    super();
    this.initRoute();
  }

  initRoute() {
    router.get('/acme-challenge/:challenge', this._onChallenge.bind(this));
  }

  _onChallenge(req, res) {
    const challenge = req.params.challenge;
    let data = challenge + this.challengeResponse;
    res.status(200).send(data);
  }
}

new LetsEncryptRoute();

module.exports = router;
