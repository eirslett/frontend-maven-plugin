String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('< Hello >') : 'gulp failed to run as expected'