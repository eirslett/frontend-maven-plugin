assert new File(basedir, 'target/node').exists() : "Node was not installed in the custom install directory";
assert new File(basedir, 'target/node/npm').exists() : "npm was not copied to the node directory";

import org.codehaus.plexus.util.FileUtils;

String buildLog = FileUtils.fileRead(new File(basedir, 'build.log'));

assert buildLog.contains('ENVIRONMENT_VARIABLE_FILE_1: 1') : 'environmentFile not working'
assert buildLog.contains('ENVIRONMENT_VARIABLE_OVERRIDE: 3') : 'environmentVariables not working overriding environmentFile'
assert buildLog.contains('ENVIRONMENT_VARIABLE_4: 4') : 'environmentVariables not working'
assert buildLog.contains('BUILD SUCCESS') : 'build was not successful'
