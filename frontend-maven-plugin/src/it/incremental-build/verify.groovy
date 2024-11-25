String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('[INFO] --- frontend-maven-plugin:1.15.1-atlassian-1-SNAPSHOT:npm (npm-install-1) @ example ---\n' +
        '[INFO] Running \'npm ci\' in') : "Invalid test, didn't install the first time"
assert new File(basedir, 'node_modules').exists() : "Invalid test, didn't install node modules";
assert buildLog.contains('[INFO] --- frontend-maven-plugin:1.15.1-atlassian-1-SNAPSHOT:npm (npm-install-2) @ example ---\n' +
        '[INFO] Running \'npm ci\' in') : "Should've re-installed the second time because the ID is different"
