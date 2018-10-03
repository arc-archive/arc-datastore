const Datastore = require('@google-cloud/datastore');
const {ArcInfoMessagesResponse} = require('../models/arc-info-resopnse-model');
/**
 * A model for catalog items.
 */
class MessagesDatastore {
  /**
   * @constructor
   */
  constructor() {
    this.store = new Datastore();
    this.namespace = 'ArcInfo';
    this.kind = 'Messages';
  }
  /**
   * Creates the datastore key with auto incremented id.
   * @return {Object} Datastore key
   */
  autoKey() {
    return this.store.key({
      namespace: this.namespace,
      path: [
        this.kind
      ]
    });
  }
  /**
   * Creates a datastore query object with options.
   * @param {Object} config Query configuration options.
   * @return {Object} Datastore query object
   */
  _createQuery(config) {
    let query = this.store.createQuery(this.namespace, this.kind)
    .order('time', {
      descending: true
    });
    if (config.cursor) {
      query = query.start(config.cursor);
    } else {
      if (config.until) {
        query = query.filter('time', '<=', config.until);
      }
      if (config.since) {
        query = query.filter('time', '>=', config.since);
      }
    }
    if (config.target) {
      query = query.filter('target', '=', config.target);
    }
    if (config.channel) {
      query = query.filter('channel', '=', config.channel);
    }
    const limit = config.limit ? config.limit : 25;
    query = query.limit(limit);
    return query;
  }
  /**
   * Prepares list of messages to be returned by the API.
   * If `channel` is set and message on the list do not have `channel` property
   * then the message is discarded.
   * @param {Array} dbResult Database response
   * @param {?String} channel Messages channel
   * @return {Array} List of processed messages.
   */
  _getMessages(dbResult, channel) {
    const entities = dbResult[0];
    for (let i = entities.length - 1; i >= 0; i--) {
      if (!channel && entities[i].channel) {
        entities.splice(i, 1);
      }
      entities[i].key = entities[i][Datastore.KEY].id;
    }
    return entities;
  }
  /**
   * Makes the query to the backend to retreive list or messages.
   * @param {Object} config Query options.
   * @return {Promise}
   */
  query(config) {
    const query = this._createQuery(config);
    return this.store.runQuery(query)
    .then((results) => {
      const entities = this._getMessages(results, config.channel);
      const info = results[1];
      const response = new ArcInfoMessagesResponse(entities,
        config.since, config.until);
      if (info.moreResults !== Datastore.NO_MORE_RESULTS) {
        response.cursor = info.endCursor;
      }
      return response;
    });
  }
  /**
   * Insets new message to the datastore.
   * @param {Array<Object>} message An entity definition as a list of values
   * @return {Promise}
   */
  insert(message) {
    const data = {
      key: this.autoKey(),
      data: message
    };
    return this.store.upsert(data);
  }
}
module.exports.MessagesDatastore = MessagesDatastore;
