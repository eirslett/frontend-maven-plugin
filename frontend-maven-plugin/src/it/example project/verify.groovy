import org.codehaus.plexus.util.FileUtils;

String buildLog = FileUtils.fileRead(new File(basedir, 'build.log'));

assert buildLog.contains('gulp runs as expected') : 'failed to run gulp as npm script'
assert buildLog.contains('BUILD SUCCESS') : 'build was not successful'