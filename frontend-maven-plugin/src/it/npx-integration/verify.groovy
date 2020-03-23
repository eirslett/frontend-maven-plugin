String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('< hello >') : 'gulp failed to run as expected'