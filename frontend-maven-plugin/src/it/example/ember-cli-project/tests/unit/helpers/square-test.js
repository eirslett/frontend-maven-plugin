import { square } from '../../../helpers/square';
import { module, test } from 'qunit';

module('SquareHelper');

test('should square a number', function(assert) {
  var result = square(3);
  assert.equal(result, 9);
});
