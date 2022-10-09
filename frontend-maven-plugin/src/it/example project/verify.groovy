import org.codehaus.plexus.util.FileUtils;

String buildLog = FileUtils.fileRead(new File(basedir, 'build.log'));

assert buildLog.contains('gulp runs as expected') : 'gulp failed to run as expected'
assert buildLog.contains('all tests passed!') : 'web-test-runner failed to run as expected'
assert buildLog.contains('4 modules transformed.') : 'build was not successful'