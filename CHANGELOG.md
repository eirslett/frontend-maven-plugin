# frontend-maven-plugin

Last public release: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.eirslett/frontend-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.eirslett/frontend-maven-plugin/)

## Changelog

### 1.12.1

* update Dependency: Jackson (2.13.0), Mockito (4.1.0), JUnit (5.8.1), Hamcrest (2.2; now a direct dependency)
* remove Dependency: Powermock
* Added better support for Yarn 2.x and above (Berry)

### 1.11.4
* Support node arm64 binaries since v16 major release

### 1.11.1

* Fix wrong binary on AIX downloaded ([#839])

### 1.11.0

* Upgrade Jackson dependency to Jackson 2.9.10
* Support Apple Silicon

### 1.10.2

* Supports Alpine Linux

### 1.9.0

* Copy npm scripts, so they are available for execution ([#868](https://github.com/eirslett/frontend-maven-plugin/pull/868))
* Regression bug fix (tar files) ([#864](https://github.com/eirslett/frontend-maven-plugin/pull/864))
* Fix bug related to archive extraction on case-insensitive file systems ([#845](https://github.com/eirslett/frontend-maven-plugin/pull/843))
* Regression bug fix (tar files) ([#816](https://github.com/eirslett/frontend-maven-plugin/pull/816))
* Added support for Raspbian OS armv7l architecture ([#809](https://github.com/eirslett/frontend-maven-plugin/pull/809))

### 1.8.0

* The plugin always logs output from npm/runners as INFO, not WARN or ERROR.
* Support for quirky Windows handling of PATH environment variables.

### 1.7.6

* Fix #670: Plugin will no longer fail to install node.exe if node.exe already exists 
* Fix #794: Plugin will self-repair if previous node/npm/yarn archive download was interrupted

### 1.5

* Revert support for the maven.frontend.failOnError flag ([#572](https://github.com/eirslett/frontend-maven-plugin/pull/572)), due to
the major regression described in [#613](https://github.com/eirslett/frontend-maven-plugin/issues/613).
failOnError-like behavior can be implemented by ignoring exit codes;
`npm run mytask` from the maven plugin, and `"scripts": { "mytask": "runstuff || exit 0"` in package.json

### 1.4

* Add maven.frontend.failOnError and maven.test.failure.ignore flags to best manage integration-test
* Fix #41: Replaced ProcessBuilder usage with commons-exec
* Use InstallDirectory to locate node tasks instead of the WorkingDirectory as fallback
* Fix 531: update lifecycle-mapping-metadata.xml for yarn
* Fix execute goal for gulp
* Fix #532: fix NullPointerException for invalid yarn version
* Added bower proxy ignore parameter
* Document how to skip package managers and build tools'

### 1.3

* Fix `yarn` for Windows
* Fix #515: Change "yarn warning" from ERROR to WARNING in log

### 1.2

* New goals `yarn` and `install-node-and-yarn` for Yarn support 

### 1.1

* Update requirements to Java 1.7
* Fix #469: Check write permissions on node installation folder
* ThreadSafe Node and NPM installation
* Add documentation and example for environmentVariables 
* Add ARM's 64bit server aarch64 support
* Add Linux on Power Systems ppc64le support
* Authenticated download
* Support for using NPM provided by node versions >4.0.0
* Fix #482: https-proxy setting from Maven

### 1.0

* Fix #384: Add parameter `npmInheritsProxyConfigFromMaven` 
* Update `maven-invoker-plugin` to v2.0.0 to fix the build on Windows mith Maven 3.2.2
  Caused by this issue: https://issues.apache.org/jira/browse/MINVOKER-166
* Fix #343: Change "npm WARN" from ERROR to WARNING in log


### 0.0.29

* Add support for caching downloaded files
* Enable SSL client certificate authentication for node download url
* Set paths in npm helper scripts so child node processes can be spawned
* Updated README with example for maven 2
* Fix #322: Use proxies more correctly
