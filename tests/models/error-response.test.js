const assert = require('chai').assert;
const {ErrorResponse} = require('../../models/error-response');

describe('ErrorResponse', function() {
  it('Creates object with defaults', () => {
    const instance = new ErrorResponse();
    assert.equal(instance.code, 400);
    assert.typeOf(instance.message, 'string');
  });

  it('Sets "code" value', () => {
    const instance = new ErrorResponse(401);
    assert.equal(instance.code, 401);
  });

  it('Sets "message" value', () => {
    const instance = new ErrorResponse(401, 'test');
    assert.equal(instance.message, 'test');
  });
});
