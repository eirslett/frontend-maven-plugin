import { test, module } from 'ember-qunit';
import { square } from 'ember-cli-project/helpers/square';

module('SquareHelper');

test('should square a number', function(assert) {
  var result = square(3);
  assert.equal(result, 9);
});
