String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('< hello >') : 'npx failed to run as expected'
assert buildLog.contains('< command >') : 'npx -c failed to run as expected'
