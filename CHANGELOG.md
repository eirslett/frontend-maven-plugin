# frontend-maven-plugin

## Changelog

### 1.4


* Add failOnError and maven.test.failure.ignore flags to best manage integration-test


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

