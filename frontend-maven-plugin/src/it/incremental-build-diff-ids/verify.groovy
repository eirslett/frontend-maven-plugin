String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('npm (npm-install-1) @ incremental-build-diff-ids ---') : "Invalid test, didn't run the first execution"
assert buildLog.contains('npm (npm-install-2) @ incremental-build-diff-ids ---') : "Invalid test, didn't run the second execution"
assert new File(basedir, 'node_modules').exists() : "Invalid test, didn't install node modules";

assert buildLog.findAll(/\[INFO\] Running 'npm ci' in/).size() == 2 : "Should've executed twice since it's a different id"
