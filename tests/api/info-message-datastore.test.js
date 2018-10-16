const assert = require('chai').assert;
const {MessagesDatastore} = require('../../lib/messages-datastore');

describe('Messages datastore', function() {
  describe('autoKey()', function() {
    let instance;
    beforeEach(() => {
      instance = new MessagesDatastore();
    });

    it('Returns an object', () => {
      const result = instance.autoKey();
      assert.typeOf(result, 'object');
    });

    it('Sets namespace', () => {
      const result = instance.autoKey();
      assert.equal(result.namespace, 'ArcInfo');
    });

    it('Sets kind', () => {
      const result = instance.autoKey();
      assert.equal(result.kind, 'Messages');
    });

    it('Sets path first property', () => {
      const result = instance.autoKey();
      assert.equal(result.path[0], 'Messages');
    });

    it('Second path property is undefined', () => {
      const result = instance.autoKey();
      assert.isUndefined(result.path[1]);
    });
  });

  describe('_createQuery()', function() {
    let instance;
    let opts;
    beforeEach(() => {
      instance = new MessagesDatastore();
      opts = {
        since: 1,
        until: 2,
        limit: 3,
        target: 'electron',
        channel: 'stable'
      };
    });

    it('Sets namespace', () => {
      const result = instance._createQuery(opts);
      assert.equal(result.namespace, 'ArcInfo');
    });

    it('Sets kind', () => {
      const result = instance._createQuery(opts);
      assert.deepEqual(result.kinds, ['Messages']);
    });

    it('Has single order definition', () => {
      const result = instance._createQuery(opts);
      assert.lengthOf(result.orders, 1);
    });

    it('Sets order by time', () => {
      const result = instance._createQuery(opts);
      const order = result.orders[0];
      assert.equal(order.name, 'time');
    });

    it('Order is descending', () => {
      const result = instance._createQuery(opts);
      const order = result.orders[0];
      assert.equal(order.sign, '-');
    });

    it('Query is not grupped by any property', () => {
      const result = instance._createQuery(opts);
      assert.lengthOf(result.groupByVal, 0);
    });

    it('Query has no start key', () => {
      const result = instance._createQuery(opts);
      assert.equal(result.startVal, null);
    });

    it('Query has no end key', () => {
      const result = instance._createQuery(opts);
      assert.equal(result.endVal, null);
    });

    it('Query has limit', () => {
      const result = instance._createQuery(opts);
      assert.equal(result.limitVal, 3);
    });

    it('Has target filter', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'target');
      assert.typeOf(filter, 'object');
    });

    it('Target filter has value', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'target');
      assert.equal(filter.val, 'electron');
    });

    it('Target filter has equality operation', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'target');
      assert.equal(filter.op, '=');
    });

    it('Has channel filter', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'channel');
      assert.typeOf(filter, 'object');
    });

    it('Channel filter has value', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'channel');
      assert.equal(filter.val, 'stable');
    });

    it('Channel filter has equality operation', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'channel');
      assert.equal(filter.op, '=');
    });

    it('Has time (since) filter', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'time' && i.op === '>=');
      assert.typeOf(filter, 'object');
    });

    it('Time (since) filter has value', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'time' && i.op === '>=');
      assert.strictEqual(filter.val, 1);
    });

    it('Has time (until) filter', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'time' && i.op === '<=');
      assert.typeOf(filter, 'object');
    });

    it('Time (until) filter has value', () => {
      const result = instance._createQuery(opts);
      const filter = result.filters.find((i) => i.name === 'time' && i.op === '<=');
      assert.strictEqual(filter.val, 2);
    });
  });
});
