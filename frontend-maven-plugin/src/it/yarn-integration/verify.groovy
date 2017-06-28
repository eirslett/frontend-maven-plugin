assert new File(basedir, 'target/node').exists() : "Node was not installed in the custom install directory";
assert new File(basedir, 'node_modules').exists() : "Node modules were not installed in the base directory";
assert new File(basedir, 'node_modules/less/package.json').exists() : "Less dependency has not been installed successfully";

String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('BUILD SUCCESS') : 'build was not successful'
assert buildLog.replace(File.separatorChar, '/' as char).matches('(?s).+Unpacking .+\\Q/local-repo/com/github/eirslett/yarn/0.16.1/yarn-0.16.1.tar.gz\\E into .+/target/node/yarn.+') : 'incorrect local repository location'
