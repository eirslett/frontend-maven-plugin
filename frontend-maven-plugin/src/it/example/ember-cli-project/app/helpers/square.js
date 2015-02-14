import Ember from 'ember';

export function square(n) {
  return n*n;
}

export default Ember.Handlebars.makeBoundHelper(square);
