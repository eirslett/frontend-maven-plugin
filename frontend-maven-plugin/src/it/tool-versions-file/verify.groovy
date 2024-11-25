assert new File(basedir, 'target/node').exists() : "Node was not installed in the custom install directory";

String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('.tool-versions') : 'The wrong file was used'
assert buildLog.contains('Installing node version v22.5.1') : 'The correct node version was not detected'
