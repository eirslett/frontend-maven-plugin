String buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Atlassian project detected, going to use the internal mirrors (requires VPN)') : "Didn't try to use PAC for an Atlassian project"
assert buildLog.contains('Could not find server \'maven-atlassian-com\' in settings.xml') : "Test is invalid from running on a machine that has the internal server setup"
assert new File(basedir, 'target/node').exists() : "Falling back didn't succeed in installing Node";
