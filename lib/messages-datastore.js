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

  autoKey() {
    return this.store.key({
      namespace: this.namespace,
      path: [
        this.kind
      ]
    });
  }

  query(config) {
    let query = this.store.createQuery(this.namespace, this.kind)
    .order('time', {
      descending: true
    });
    if (config.cursor) {
      query = query.start(config.cursor);
    } else {
      query = query.filter('time', '<=', config.until)
        .filter('time', '>=', config.since);
    }
    if (config.target) {
      query = query.filter('target', '=', config.target);
    }
    if (config.channel) {
      query = query.filter('channel', '=', config.channel);
    }
    return this.store.runQuery(query)
    .then((results) => {
      const entities = results[0];
      if (!config.channel && entities && entities.length) {
        for (let i = entities.length - 1; i >= 0; i--) {
          if (entities[i].channel) {
            entities.splice(i, 1);
          }
        }
      }
      const info = results[1];
      const response = new ArcInfoMessagesResponse(entities,
        config.since, config.until);
      if (info.moreResults !== Datastore.NO_MORE_RESULTS) {
        response.cursor = info.endCursor;
      }
      return response;
    });
  }

  insert(message) {
    const data = {
      key: this.autoKey(),
      data: message
    };
    return this.store.upsert(data);
  }
}
module.exports.MessagesDatastore = MessagesDatastore;
