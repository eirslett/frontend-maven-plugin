import {
  square
} from 'ember-cli-project/helpers/square';

module('SquareHelper');

test('should square a number', function() {
  var result = square(3);
  equal(result, 9);
});
