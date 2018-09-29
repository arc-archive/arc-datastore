'use strict';

// Activate Google Cloud Trace and Debug when in production
if (process.env.NODE_ENV === 'production') {
  require('@google-cloud/trace-agent').start();
  require('@google-cloud/debug-agent').start({
    allowExpressions: true
  });
}

const {ErrorReporting} = require('@google-cloud/error-reporting');
const path = require('path');
const logger = require('morgan');
const logging = require('./lib/logging');
const express = require('express');
const bb = require('express-busboy');
const session = require('express-session');
const passport = require('passport');
const config = require('./config');

const app = express();
app.enable('trust proxy');
app.disable('etag');

app.set('view engine', 'pug');
app.set('views', path.join(__dirname, 'views'));

app.use(logger('dev'));
bb.extend(app); // For multipart queries.
app.use(express.static(path.join(__dirname, 'public')));

// https://github.com/GoogleCloudPlatform/nodejs-getting-started/blob/master/4-auth/app.js
// Configure the session and session storage.
const sessionConfig = {
  resave: false,
  saveUninitialized: false,
  secret: config.get('SECRET'),
  signed: true
};
app.use(session(sessionConfig));

// OAuth2
app.use(passport.initialize());
app.use(passport.session());
app.use(require('./lib/oauth2').router);

app.use('/analyzer', require('./routes/analyzer'));
app.use('/analytics', require('./routes/analytics'));
app.use('/.well-known/', require('./routes/letsencrypt'));
app.use('/tasks/', require('./routes/tasks'));
app.use('/info/', require('./routes/info'));
app.use('/_ah/', require('./routes/ah'));

app.set('x-powered-by', false);

app.use(logging.requestLogger);

// catch 404 and forward to error handler
app.use((req, res, next) => {
  const err = new Error('Not Found');
  err.status = 404;
  next(err);
});

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use((err, req, res) => {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: err
    });
  });
}

// production error handler
// no stacktraces leaked to user
app.use((err, req, res) => {
  const errors = new ErrorReporting();
  errors.report(err);
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: {}
  });
});

module.exports = app;
