'use strict';

const express = require('express');
const config = require('../config');

const passport = require('passport');
const GoogleStrategy = require('passport-google-oauth20').Strategy;

function extractProfile(profile) {
  let imageUrl = '';
  if (profile.photos && profile.photos.length) {
    imageUrl = profile.photos[0].value;
  }
  return {
    id: profile.id,
    displayName: profile.displayName,
    image: imageUrl
  };
}

function isAdminUser(id) {
  return ['113648393775261658002'].indexOf(id) !== -1;
}

// OAuth 2-based strategies require a `verify` function which receives the
// credential (`accessToken`) for accessing the Google API on the user's behalf,
// along with the user's profile. The function must invoke `cb` with a user
// object, which will be set at `req.user` in route handlers after
// authentication.
passport.use(new GoogleStrategy({
  clientID: config.get('OAUTH2_CLIENT_ID'),
  clientSecret: config.get('OAUTH2_CLIENT_SECRET'),
  callbackURL: config.get('OAUTH2_CALLBACK'),
  accessType: 'offline'
}, (accessToken, refreshToken, profile, cb) => {
  // Extract the minimal profile information we need from the profile object
  // provided by Google
  cb(null, extractProfile(profile));
}));

passport.serializeUser((user, cb) => {
  cb(null, user);
});

passport.deserializeUser((obj, cb) => {
  cb(null, obj);
});

const router = express.Router();

// Middleware that requires the user to be logged in. If the user is not logged
// in, it will redirect the user to authorize the application and then return
// them to the original URL they requested.
function authRequired(req, res, next) {
  if (!req.user) {
    res.status(401);
    res.render('error', {
      message: 'Authorization is required. Visit /auth/login',
      error: {}
    });
    // req.session.oauth2return = req.originalUrl;
    // return res.redirect('/auth/login');
  }
  next();
}

router.get(
  // Login url
  '/auth/login',
  // Save the url of the user's current page so the app can redirect back to
  // it after authorization
  (req, res, next) => {
    if (req.query.return) {
      req.session.oauth2return = req.query.return;
    }
    next();
  },
  // Start OAuth 2 flow using Passport.js
  passport.authenticate('google', {scope: ['email', 'profile']})
);

router.get(
  // OAuth 2 callback url. Use this url to configure your OAuth client in the
  // Google Developers console
  '/auth/callback',
  // Finish OAuth 2 flow using Passport.js
  passport.authenticate('google'),
  // Redirect back to the original page, if any
  (req, res) => {
    const redirect = req.session.oauth2return || '/';
    delete req.session.oauth2return;
    res.redirect(redirect);
  }
);

router.get('/auth/tokeninfo', authRequired, (req, res) => {
  if (req.user) {
    const body = JSON.stringify({
      id: req.user.id
    }, null, 2);
    res.set('Content-Type', 'application/json');
    res.status(200).send(body);
    return;
  }
});

router.get('/auth/logout', (req, res) => {
  req.logout();
  res.redirect('/');
});

module.exports = {
  extractProfile: extractProfile,
  router: router,
  required: authRequired,
  isAdminUser: isAdminUser
};
