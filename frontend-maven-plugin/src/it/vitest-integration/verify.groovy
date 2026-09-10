String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('1 passed') : 'vitest failed to run as expected'
assert buildLog.contains('BUILD SUCCESS'): 'build was not successful'
