const assert = require('chai').assert;
const {InfoRoute} = require('../../../routes/info');

describe('Create message', function() {
  describe('_readQueryParams()', function() {
    let instance;
    let opts;
    beforeEach(() => {
      instance = new InfoRoute();
      opts = {
        since: '1',
        until: '2',
        limit: '3',
        platform: 'electron',
        channel: 'stable'
      };
    });

    it('Returns only cursor if present', () => {
      opts.cursor = 'test';
      const result = instance._readQueryParams(opts);
      assert.equal(result.cursor, 'test');
      assert.isUndefined(result.since);
      assert.isUndefined(result.until);
      assert.isUndefined(result.limit);
    });

    it('Sets since property', () => {
      const result = instance._readQueryParams(opts);
      assert.strictEqual(result.since, 1);
    });

    it('Sets until property', () => {
      const result = instance._readQueryParams(opts);
      assert.strictEqual(result.until, 2);
    });

    it('Sets limit property', () => {
      const result = instance._readQueryParams(opts);
      assert.strictEqual(result.limit, 3);
    });

    it('Throws error when since is not a number', () => {
      opts.since = 'nan';
      assert.throws(() => {
        instance._readQueryParams(opts);
      });
    });

    it('Throws error when until is not a number', () => {
      opts.until = 'nan';
      assert.throws(() => {
        instance._readQueryParams(opts);
      });
    });

    it('Throws error when limit is not a number', () => {
      opts.limit = 'nan';
      assert.throws(() => {
        instance._readQueryParams(opts);
      });
    });

    it('Sets "target" property', () => {
      const result = instance._readQueryParams(opts);
      assert.strictEqual(result.target, 'electron');
    });

    it('Sets "channel" property', () => {
      const result = instance._readQueryParams(opts);
      assert.strictEqual(result.channel, 'stable');
    });
  });

  describe('_bodyToMessageProperties()', function() {
    let instance;
    let opts;
    beforeEach(() => {
      instance = new InfoRoute();
      opts = {
        abstract: 'test-abstract',
        actionurl: 'test-actionurl',
        cta: 'test-cta',
        target: 'test-target',
        channel: 'test-channel',
        title: 'test-title'
      };
    });
    ['abstract', 'actionurl', 'cta', 'target', 'title', 'channel']
    .forEach((prop) => {
      it(`Throws error when "${prop}" is missing`, () => {
        delete opts[prop];
        assert.throws(() => {
          instance._bodyToMessageProperties(opts);
        });
      });
    });

    it(`Throws error when "time" is not a number`, () => {
      opts.time = 'nan';
      assert.throws(() => {
        instance._bodyToMessageProperties(opts);
      });
    });

    it(`Returns an array`, () => {
      const result = instance._bodyToMessageProperties(opts);
      assert.typeOf(result, 'array');
    });

    it(`Sets time if missing`, () => {
      const result = instance._bodyToMessageProperties(opts);
      const property = result.find((i) => i.name === 'time');
      assert.typeOf(property, 'object');
      assert.typeOf(property.value, 'number');
    });

    it(`Parses time value to a number`, () => {
      opts.time = '1234';
      const result = instance._bodyToMessageProperties(opts);
      const property = result.find((i) => i.name === 'time');
      assert.strictEqual(property.value, 1234);
    });

    [
      ['abstract', 'test-abstract'],
      ['actionurl', 'test-actionurl'],
      ['cta', 'test-cta'],
      ['title', 'test-title'],
      ['channel', 'test-channel'],
      ['title', 'test-title']
    ].forEach((item) => {
      it(`Sets ${item[0]} property`, () => {
        const result = instance._bodyToMessageProperties(opts);
        const property = result.find((i) => i.name === item[0]);
        assert.typeOf(property, 'object');
        assert.equal(property.value, item[1]);
      });
    });

    ['abstract', 'actionurl', 'cta', 'title']
    .forEach((prop) => {
      it(`Excludes ${prop} from index`, () => {
        const result = instance._bodyToMessageProperties(opts);
        const property = result.find((i) => i.name === prop);
        assert.isTrue(property.excludeFromIndexes);
      });
    });

    ['target', 'time', 'channel']
    .forEach((prop) => {
      it(`Not excludes ${prop} from index`, () => {
        const result = instance._bodyToMessageProperties(opts);
        const property = result.find((i) => i.name === prop);
        assert.isUndefined(property.excludeFromIndexes);
      });
    });
  });
});
