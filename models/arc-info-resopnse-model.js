'use strict';
/**
 * A class representing result for daily data query.
 *
 * This class should be extended by class that returns specific results.
 */
class ArcInfoMessagesResponse {
  /**
   * @param {Array<Object>} messages List of messages
   * @param {Number} since Timestamp of earlies message
   * @param {Number} until Timestamp of latest message
   */
  constructor(messages, since, until) {
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
    this.since = since || 0;
    /**
     * Computes lowerst timestamp
     * @type {Number}
     */
    this.until = until || 0;
    /**
     * Page cursor for data store queries.
     * @type {String|undefined}
     */
    this.cursor = undefined;
    if (!since) {
      this._computeTimes(messages);
    }
  }

  _computeTimes(messages) {
    let since;
    let until;
    messages.forEach((message) => {
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
