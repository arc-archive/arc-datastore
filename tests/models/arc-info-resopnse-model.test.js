const assert = require('chai').assert;
const {ArcInfoMessagesResponse} = require('../../models/arc-info-resopnse-model.js');

describe('ArcInfoMessagesResponse', function() {
  const messages = [{
    time: 1
  }, {
    time: 2
  }];
  it('Sets "kind" property', () => {
    const instance = new ArcInfoMessagesResponse(messages);
    assert.equal(instance.kind, 'ArcInfo#MessagesList');
  });

  it('Sets "since" value from arguments', () => {
    const instance = new ArcInfoMessagesResponse(messages, 1000);
    assert.equal(instance.since, 1000);
  });

  it('Sets "until" value from arguments', () => {
    const instance = new ArcInfoMessagesResponse(messages, 1000, 2000);
    assert.equal(instance.until, 2000);
  });

  it('Sets "since" value from messages', () => {
    const instance = new ArcInfoMessagesResponse(messages);
    assert.equal(instance.since, 2);
  });

  it('Sets "until" value from arguments', () => {
    const instance = new ArcInfoMessagesResponse(messages);
    assert.equal(instance.until, 1);
  });
});
