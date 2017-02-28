var gulp = require('gulp');

gulp.task('default', function() {
  if(process.env.NODE_ENV === 'production') {
    console.log('gulp runs as expected on production');
  } else {
    console.log('gulp runs as expected');
  }
});