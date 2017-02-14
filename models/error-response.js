'use strict';

/**
 * Error report from the server.
 */
class ErrorResponse {
  /**
   * To construct error response provide a status `code` and the reason `message`.
   *
   * @param {Number} code Status code associated with this response.
   * @param {String} message A reason message.
   */
  constructor(code, message) {
    this.code = code || 400;
    this.message = message || 'Unknown error ocurred';
  }
}

module.exports.ErrorResponse = ErrorResponse;
