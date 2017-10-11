'use strict';

/**
 * A class representing result for daily data query.
 *
 * This class should be extended by class that returns specific results.
 */
class ArcInfoMessagesResponse {

  constructor(messages) {
    this.kind = 'ArcInfo#MessagesList';
    /**
     * List of messages.
     * @type {Array<Object>}
     */
    this.data = messages;
    /**
     * Computed highest timestamp
     * @type {Number}
     */
    this.since = 0;
    /**
     * Computes lowerst timestamp
     * @type {Number}
     */
    this.until = 0;
    /**
     * Page cursor for data store queries.
     * @type {String|undefined}
     */
    this.cursor = undefined;
    this._computeTimes(messages);
  }

  _computeTimes(messages) {
    var since;
    var until;
    messages.forEach(message => {
      let t = message.time;
      if (!since || since < t) {
        since = t;
      }
      if (!until || until > t) {
        until = t;
      }
    });
    this.since = since;
    this.until = until;
  }
}

module.exports.ArcInfoMessagesResponse = ArcInfoMessagesResponse;
