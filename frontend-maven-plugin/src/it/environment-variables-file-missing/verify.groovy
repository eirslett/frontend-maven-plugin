assert new File(basedir, 'target/node').exists() : "Node was not installed in the custom install directory";
assert new File(basedir, 'target/node/npm').exists() : "npm was not copied to the node directory";

import org.codehaus.plexus.util.FileUtils;

String buildLog = FileUtils.fileRead(new File(basedir, 'build.log'));

assert buildLog.contains("File containing environment variables (configuration 'environmentFile') at '${basedir}/.env' could not be found, skipping it." as CharSequence) : 'environmentFile not missing'
assert buildLog.contains('ENVIRONMENT_VARIABLE_FILE_1: undefined') : 'environmentFile should not working'
assert buildLog.contains('BUILD SUCCESS') : 'build was not successful'
