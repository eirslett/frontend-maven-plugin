import org.codehaus.plexus.util.FileUtils;

String buildLog = FileUtils.fileRead(new File(basedir, 'build.log'));

assert buildLog.contains('gulp runs as expected') : 'gulp failed to run as expected'
assert buildLog.contains('Running against local jspm install.') : 'jspm failed to run as expected'
assert buildLog.contains('6 files lint free.') : 'grunt failed to run as expected'
assert buildLog.contains('Executed 1 of 1 SUCCESS') : 'karma failed to run as expected'
assert buildLog.contains('1 spec, 0 failures') : 'protractor failed to run as expected'
assert buildLog.contains('Running 1 instances of WebDriver') : 'webdriver was not started successful'
assert buildLog.contains('BUILD SUCCESS') : 'build was not successful'