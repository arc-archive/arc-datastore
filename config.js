'use strict';

// Hierarchical node.js configuration with command-line arguments, environment
// variables, and files.
const nconf = module.exports = require('nconf');
const path = require('path');

function checkConfig(setting) {
  if (!nconf.get(setting)) {
    throw new Error(`You must set ${setting} as an environment variable or in config.json!`);
  }
}

nconf
  // 1. Command-line arguments
  .argv()
  // 2. Environment variables
  .env([
    // 'CLOUD_BUCKET',
    // 'DATA_BACKEND',
    // 'GCLOUD_PROJECT',
    // 'MEMCACHE_URL',
    // 'MEMCACHE_USERNAME',
    // 'MEMCACHE_PASSWORD',
    // 'INSTANCE_CONNECTION_NAME',
    'NODE_ENV',
    'OAUTH2_CLIENT_ID',
    'OAUTH2_CLIENT_SECRET',
    'OAUTH2_CALLBACK',
    'PORT',
    'SECRET'
  ])
  // 3. Config file
  .file({file: path.join(__dirname, 'config.json')})
  // 4. Defaults
  .defaults({
    // CLOUD_BUCKET: '',
    DATA_BACKEND: 'datastore',

    OAUTH2_CLIENT_ID: '',
    OAUTH2_CLIENT_SECRET: '',
    OAUTH2_CALLBACK: '',

    PORT: 8180
  });

// Check for required settings
// checkConfig('GCLOUD_PROJECT');
// checkConfig('CLOUD_BUCKET');
checkConfig('OAUTH2_CLIENT_ID');
checkConfig('OAUTH2_CLIENT_SECRET');
checkConfig('OAUTH2_CALLBACK');
