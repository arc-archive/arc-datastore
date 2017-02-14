// http://stackoverflow.com/a/32749533/1127848
class ExtendableError extends Error {
  constructor(message) {
    super(message);
    this.name = this.constructor.name;
    if (typeof Error.captureStackTrace === 'function') {
      Error.captureStackTrace(this, this.constructor);
    } else {
      this.stack = (new Error(message)).stack;
    }
  }
}

class ComputationRecordExistsError extends ExtendableError {}

module.exports.ComputationRecordExistsError = ComputationRecordExistsError;
