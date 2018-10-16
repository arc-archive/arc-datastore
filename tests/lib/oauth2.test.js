const assert = require('chai').assert;
const oauth2 = require('../../lib/oauth2');

describe('Oauth 2 route', function() {
  describe('extractProfile()', function() {
    let data;
    beforeEach(() => {
      data = {
        id: 'test-id',
        displayName: 'test-dn',
        other: 'test-other'
      };
    });

    it('Returns an object', () => {
      const result = oauth2.extractProfile(data);
      assert.typeOf(result, 'object');
    });

    it('Contains the ID', () => {
      const result = oauth2.extractProfile(data);
      assert.equal(result.id, 'test-id');
    });

    it('Contains display name', () => {
      const result = oauth2.extractProfile(data);
      assert.equal(result.displayName, 'test-dn');
    });

    it('Does not contain other properties', () => {
      const result = oauth2.extractProfile(data);
      assert.isUndefined(result.other);
    });
  });

  describe('router', function() {
    it('Router is an function', () => {
      assert.typeOf(oauth2.router, 'function');
    });
  });

  describe('isAdminUser()', function() {
    it('Is true for me :)', () => {
      assert.isTrue(oauth2.isAdminUser('113648393775261658002'));
    });

    it('Is false for others', () => {
      assert.isFalse(oauth2.isAdminUser('1'));
    });
  });
});
