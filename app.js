'use strict';

// Activate Google Cloud Trace and Debug when in production
if (process.env.NODE_ENV === 'production') {
  require('@google-cloud/trace-agent').start();
  require('@google-cloud/debug-agent').start({
    allowExpressions: true,
    capture: {
      maxFrames: 20,
      maxProperties: 100
    }
  });
}

const errors = require('@google-cloud/error-reporting')();
const express = require('express');
const path = require('path');
const logger = require('morgan');
const bb = require('express-busboy');
const logging = require('./lib/logging');

const app = express();
app.enable('trust proxy');
app.disable('etag');

app.set('view engine', 'pug');
app.set('views', path.join(__dirname, 'views'));

app.use(logger('dev'));
bb.extend(app); // For multipart queries.
app.use(express.static(path.join(__dirname, 'public')));

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
  console.log(req.url);
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
  errors.report(err);
  res.status(err.status || 500);
  res.render('error', {
    message: err.message,
    error: {}
  });
});

module.exports = app;
