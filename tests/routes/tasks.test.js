const assert = require('chai').assert;
const {TasksRoute} = require('../../routes/tasks');

describe('TasksRoute', function() {
  describe('_getServiceUrl()', function() {
    const TYPE_DAILY = 'daily';
    const TYPE_WEEKLY = 'weekly';
    const TYPE_MONTHLY = 'monthly';
    const SCOPE_USERS = 'users';
    const SCOPE_SESSIONS = 'sessions';

    before(() => {
      process.env.NODE_ENV = 'production';
    });

    it('Has base URL', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_DAILY, SCOPE_USERS);
      assert.equal(result.indexOf('https://advancedrestclient-1155.appspot.com/analyzer/'), 0);
    });

    it('Has base URL when not in production', () => {
      process.env.NODE_ENV = 'dev';
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_DAILY, SCOPE_USERS);
      assert.equal(result.indexOf('http://localhost:8080/analyzer/'), 0);
    });

    it('URL has users scope', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_DAILY, SCOPE_USERS);
      assert.isAbove(result.indexOf('/users?'), 0);
    });

    it('URL has sessions scope', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_DAILY, SCOPE_SESSIONS);
      assert.isAbove(result.indexOf('/sessions?'), 0);
    });

    it('URL has daily type', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_DAILY, SCOPE_SESSIONS);
      assert.isAbove(result.indexOf('/daily/'), 0);
    });

    it('URL has weekly type', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_WEEKLY, SCOPE_SESSIONS);
      assert.isAbove(result.indexOf('/weekly/'), 0);
    });

    it('URL has monthly type', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_MONTHLY, SCOPE_SESSIONS);
      assert.isAbove(result.indexOf('/monthly/'), 0);
    });

    it('Daily has yesterday\'s date', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_DAILY, SCOPE_USERS);
      const d = new Date();
      d.setDate(d.getDate() - 1);
      const compare = d.toISOString().split('T')[0];
      assert.isAbove(result.indexOf(compare), 0);
    });

    it('Weekly has last weeks\'s date', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_WEEKLY, SCOPE_USERS);
      const d = new Date();
      d.setDate(d.getDate() - 7);
      const compare = d.toISOString().split('T')[0];
      assert.isAbove(result.indexOf(compare), 0);
    });

    it('Monthly has last month\'s date', () => {
      const instance = new TasksRoute();
      const result = instance._getServiceUrl(TYPE_MONTHLY, SCOPE_USERS);
      const d = new Date();
      d.setMonth(d.getMonth() - 1);
      const compare = d.toISOString().split('T')[0];
      assert.isAbove(result.indexOf(compare), 0);
    });
  });
});
