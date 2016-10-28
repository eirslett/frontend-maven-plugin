# frontend-maven-plugin

[![Build Status OSX](https://travis-ci.org/eirslett/frontend-maven-plugin.png?branch=master)](https://travis-ci.org/eirslett/frontend-maven-plugin)
[![Build status Linux](https://eirslett.ci.cloudbees.com/buildStatus/icon?job=Frontend%20maven%20plugin)](https://eirslett.ci.cloudbees.com/job/Frontend%20maven%20plugin/)
[![Build status Windows](https://ci.appveyor.com/api/projects/status/vxbccc1t9ceadhi9?svg=true)](https://ci.appveyor.com/project/eirslett/frontend-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.eirslett/frontend-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.eirslett/frontend-maven-plugin/)

This plugin downloads/installs Node and NPM locally for your project, runs `npm install`, and then any combination of 
[Bower](http://bower.io/), [Grunt](http://gruntjs.com/), [Gulp](http://gulpjs.com/), [Jspm](http://jspm.io), 
[Karma](http://karma-runner.github.io/), or [Webpack](http://webpack.github.io/).
It's supposed to work on Windows, OS X and Linux.

If you prefer [Yarn](https://yarnpkg.com/) over [NPM](https://www.npmjs.com/) for your node package fetching, 
this plugin can also download Node and Yarn and then run `yarn install` for your project.

#### What is this plugin meant to do?
- Let you keep your frontend and backend builds as separate as possible, by
reducing the amount of interaction between them to the bare minimum; using only 1 plugin.
- Let you use Node.js and its libraries in your build process without installing Node/NPM
globally for your build system
- Let you ensure that the version of Node and NPM being run is the same in every build environment

#### What is this plugin not meant to do?
- Not meant to replace the developer version of Node - frontend developers will still install Node on their
laptops, but backend developers can run a clean build without even installing Node on their computer.
- Not meant to install Node for production uses. The Node usage is intended as part of a frontend build,
running common javascript tasks such as minification, obfuscation, compression, packaging, testing etc.

**Notice:** _This plugin does not support already installed Node or npm versions. Use the `exec-maven-plugin` instead._

## Requirements

* _Maven 3_ and _Java 1.7_
* For _Maven 2_ support take a look at the [wiki](https://github.com/eirslett/frontend-maven-plugin/wiki#maven-2).

## Installation

Include the plugin as a dependency in your Maven project. Change `LATEST_VERSION` to the latest tagged version.

```xml
<plugins>
    <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <!-- Use the latest released version:
        https://repo1.maven.org/maven2/com/github/eirslett/frontend-maven-plugin/ -->
        <version>LATEST_VERSION</version>
        ...
    </plugin>
...
```

## Usage

Have a look at the [example project](https://github.com/eirslett/frontend-maven-plugin/tree/master/frontend-maven-plugin/src/it/example%20project), 
to see how it should be set up: https://github.com/eirslett/frontend-maven-plugin/blob/master/frontend-maven-plugin/src/it/example%20project/pom.xml

 - [Installing node and npm](#installing-node-and-npm)
 - [Installing node and yarn](#installing-node-and-yarn)
 - Running 
    - [npm](#running-npm)
    - [yarn](#running-yarn)
    - [bower](#running-bower)
    - [grunt](#running-grunt)
    - [gulp](#running-gulp)
    - [jspm](#running-jspm)
    - [karma](#running-karma)
    - [webpack](#running-webpack)
 - Configuration
    - [Working Directory](#working-directory)
    - [Installation Directory](#installation-directory)
    - [Proxy Settings](#proxy-settings)
    - [Environment variables](#environment-variables)
    
**Recommendation:** _Try to run all your tasks via npm scripts instead of running bower, grunt, gulp etc. directly._

### Installing node and npm

The versions of Node and npm are downloaded from https://nodejs.org/dist, extracted and put into a `node` folder created 
in your [installation directory](#installation-directory) . Node/npm will only be "installed" locally to your project. 
It will not be installed globally on the whole system (and it will not interfere with any Node/npm installations already 
present). 

```xml
<plugin>
    ...
    <execution>
        <!-- optional: you don't really need execution ids, but it looks nice in your build log. -->
        <id>install node and npm</id>
        <goals>
            <goal>install-node-and-npm</goal>
        </goals>
        <!-- optional: default phase is "generate-resources" -->
        <phase>generate-resources</phase>
    </execution>
    <configuration>
        <nodeVersion>v4.6.0</nodeVersion>

        <!-- optional: with node version greater than 4.0.0 will use npm provided by node distribution -->
        <npmVersion>2.15.9</npmVersion>
        
        <!-- optional: where to download node and npm from. Defaults to https://nodejs.org/dist/ -->
        <downloadRoot>http://myproxy.example.org/nodejs/</downloadRoot>
    </configuration>
</plugin>
```

You can also specify separate download roots for npm and node as they are stored in separate repos.

```xml
<plugin>
    ...
    <configuration>
        <!-- optional: where to download node from. Defaults to https://nodejs.org/dist/ -->
        <nodeDownloadRoot>http://myproxy.example.org/nodejs/</nodeDownloadRoot>
        <!-- optional: where to download npm from. Defaults to http://registry.npmjs.org/npm/-/ -->
        <npmDownloadRoot>https://myproxy.example.org/npm/</npmDownloadRoot>
    </configuration>
</plugin>
```

You can use Nexus repository Manager to proxy npm registries. See https://books.sonatype.com/nexus-book/reference/npm.html

**Notice:** _Remember to gitignore the `node` folder, unless you actually want to commit it._

### Installing node and yarn

Instead of using Node with npm you can alternatively choose to install Node with Yarn as the package manager.

The versions of Node and Yarn are downloaded from `https://nodejs.org/dist` for Node 
and from the Github releases for Yarn, 
extracted and put into a `node` folder created in your installation directory. 
Node/Yarn will only be "installed" locally to your project. 
It will not be installed globally on the whole system (and it will not interfere with any Node/Yarn installations already 
present). 

Have a look at the example `POM` to see how it should be set up with Yarn: 
https://github.com/eirslett/frontend-maven-plugin/blob/master/frontend-maven-plugin/src/it/yarn-integration/pom.xml


```xml
<plugin>
    ...
    <execution>
        <!-- optional: you don't really need execution ids, but it looks nice in your build log. -->
        <id>install node and yarn</id>
        <goals>
            <goal>install-node-and-yarn</goal>
        </goals>
        <!-- optional: default phase is "generate-resources" -->
        <phase>generate-resources</phase>
    </execution>
    <configuration>
        <nodeVersion>v6.9.1</nodeVersion>
        <yarnVersion>v0.16.1</yarnVersion>

        <!-- optional: where to download node from. Defaults to https://nodejs.org/dist/ -->
        <nodeDownloadRoot>http://myproxy.example.org/nodejs/</nodeDownloadRoot>
        <!-- optional: where to download yarn from. Defaults to https://github.com/yarnpkg/yarn/releases/download/ -->
        <yarnDownloadRoot>http://myproxy.example.org/yarn/</yarnDownloadRoot>        
    </configuration>
</plugin>
```

### Running npm

All node packaged modules will be installed in the `node_modules` folder in your [working directory](#working-directory).
By default, colors will be shown in the log.

```xml
<execution>
    <id>npm install</id>
    <goals>
        <goal>npm</goal>
    </goals>

    <!-- optional: default phase is "generate-resources" -->
    <phase>generate-resources</phase>

    <configuration>
        <!-- optional: The default argument is actually
        "install", so unless you need to run some other npm command,
        you can remove this whole <configuration> section.
        -->
        <arguments>install</arguments>
    </configuration>
</execution>
```

**Notice:** _Remember to gitignore the `node_modules` folder, unless you actually want to commit it. Npm packages will 
always be installed in `node_modules` next to your `package.json`, which is default npm behavior._

### Running yarn

As with npm above, all node packaged modules will be installed in the `node_modules` folder in your [working directory](#working-directory).


```xml
<execution>
    <id>yarn install</id>
    <goals>
        <goal>yarn</goal>
    </goals>
    <configuration>
         <!-- optional: The default argument is actually
         "install", so unless you need to run some other yarn command,
         you can remove this whole <configuration> section.
         -->
        <arguments>install</arguments>
    </configuration>
</execution>
```

### Running bower

All bower dependencies will be installed in the `bower_components` folder in your working directory.

```xml
<execution>
    <id>bower install</id>
    <goals>
        <goal>bower</goal>
    </goals>

    <configuration>
        <!-- optional: The default argument is actually
        "install", so unless you need to run some other bower command,
        you can remove this whole <configuration> section.
        -->
        <arguments>install</arguments>
    </configuration>
</execution>
```

**Notice:** _Remember to gitignore the `bower_components` folder, unless you actually want to commit it._

### Running Grunt

It will run Grunt according to the `Gruntfile.js` in your working directory.
By default, colors will be shown in the log.

```xml
<execution>
    <id>grunt build</id>
    <goals>
        <goal>grunt</goal>
    </goals>

    <!-- optional: the default phase is "generate-resources" -->
    <phase>generate-resources</phase>

    <configuration>
        <!-- optional: if not specified, it will run Grunt's default
        task (and you can remove this whole <configuration> section.) -->
        <arguments>build</arguments>
    </configuration>
</execution>
```

### Running gulp

Very similar to the Grunt execution. It will run gulp according to the `gulpfile.js` in your working directory.
By default, colors will be shown in the log.

```xml
<execution>
    <id>gulp build</id>
    <goals>
        <goal>gulp</goal>
    </goals>

    <!-- optional: the default phase is "generate-resources" -->
    <phase>generate-resources</phase>

    <configuration>
        <!-- optional: if not specified, it will run gulp's default
        task (and you can remove this whole <configuration> section.) -->
        <arguments>build</arguments>
    </configuration>
</execution>
```

### Running jspm

All jspm dependencies will be installed in the `jspm_packages` folder in your working directory.

```xml
<execution>
    <id>jspm install</id>
    <goals>
        <goal>jspm</goal>
    </goals>

    <configuration>
	    <!-- optional: The default argument is actually
	    "install", so unless you need to run some other jspm command,
	    you can remove this whole <configuration> section.
	    -->
        <arguments>install</arguments>
    </configuration>
</execution>
```

### Running Karma

```xml
<execution>
    <id>javascript tests</id>
    <goals>
        <goal>karma</goal>
    </goals>

    <!-- optional: the default plase is "test". Some developers
    choose to run karma in the "integration-test" phase. -->
    <phase>test</phase>

    <configuration>
        <!-- optional: the default is "karma.conf.js" in your working directory -->
        <karmaConfPath>src/test/javascript/karma.conf.ci.js</karmaConfPath>
    </configuration>
</execution>
```

**Skipping tests:** If you run maven with the `-DskipTests` flag, karma tests will be skipped.

**Ignoring failed tests:** If you want to ignore test failures run maven with the `-Dmaven.test.failure.ignore` flag, 
karma test results will not stop the build but test results will remain
in test output files. Suitable for continuous integration tool builds.

**Why karma.conf.ci.js?** When using Karma, you should have two separate
configurations: `karma.conf.js` and `karma.conf.ci.js`. (The second one should inherit configuration
from the first one, and override some options. The example project shows you how to set it up.)
The idea is that you use `karma.conf.js` while developing (using watch/livereload etc.), and
`karma.conf.ci.js` when building - for example, when building, it should only run karma once,
it should generate xml reports, it should run only in PhantomJS, and/or it should generate
code coverage reports.

**Running Karma through Grunt or gulp:** You may choose to run Karma [directly through Grunt](https://github.com/karma-runner/grunt-karma) 
or [through gulp](https://github.com/karma-runner/gulp-karma) instead, as part of the `grunt` or `gulp` execution. That 
will help to separate your frontend and backend builds even more.

### Running Webpack

```xml
<execution>
    <id>webpack build</id>
    <goals>
        <goal>webpack</goal>
    </goals>

    <!-- optional: the default phase is "generate-resources" -->
    <phase>generate-resources</phase>

    <configuration>
        <!-- optional: if not specified, it will run webpack's default
        build (and you can remove this whole <configuration> section.) -->
        <arguments>-p</arguments>
    </configuration>
</execution>
```

### Optional Configuration 

#### Working directory

The working directory is where you've put `package.json` and your frontend configuration files (`Gruntfile.js` or 
`gulpfile.js` etc). The default working directory is your project's base directory (the same directory as your `pom.xml`). 
You can change the working directory if you want:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>

    <!-- optional -->
    <configuration>
        <workingDirectory>src/main/frontend</workingDirectory>
    </configuration>
</plugin>
```

**Notice:** _Npm packages will always be installed in `node_modules` next to your `package.json`, which is default npm behavior._

#### Installation Directory

The installation directory is the folder where your node and npm are installed.
You can set this property on the different goals. Or choose to set it for all the goals, in the maven configuration.

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>

    <!-- optional -->
    <configuration>
        <installDirectory>target</installDirectory>
    </configuration>    
</plugin>
```

#### Proxy settings

If you have [configured proxy settings for Maven](http://maven.apache.org/guides/mini/guide-proxies.html)
in your settings.xml file, the plugin will automatically use the proxy for downloading node and npm, as well
as [passing the proxy to npm commands](https://docs.npmjs.com/misc/config#proxy).

**Non Proxy Hosts:** npm does not currently support non proxy hosts - if you are using a proxy and npm install is 
is not downloading from your repository, it may be because it cannot be accessed through your proxy. 
If that is the case, you can stop the npm execution from inheriting the Maven proxy settings like this:

```xml
<configuration>
    <npmInheritsProxyConfigFromMaven>false</npmInheritsProxyConfigFromMaven>
</configuration>
```

#### Environment variables

If you need to pass some variable to Node, you can set that using the property `environmentVariables` in configuration 
tag of an execution like this:

```xml
<configuration>
    <environmentVariables>
        <!-- Simple var -->
        <Jon>Snow</Jon>
        <Tyrion>Lannister</Tyrion>
        
        <!-- Var value take from maven properties -->
        <NODE_ENV>${NODE_ENV}</NODE_ENV>
    </environmentVariables>        
</configuration>
```


## Eclipse M2E support

This plugin contains support for M2E, including lifecycle mappings and support for incremental builds in Eclipse.
The `install-node-and-npm` goal will only run on a full project build. The other goals support incremental builds
to avoid doing unnecessary work. During an incremental build the `npm` goal will only run if the `package.json` file
has been changed. The `grunt` and `gulp` goals have new `srcdir` and `triggerfiles` optional configuration options; if
these are set they check for changes in your source files before being run. See the wiki for more information.

## Helper scripts

During development, it's convenient to have the "npm", "bower", "grunt", "gulp" and "karma" commands
available on the command line. If you want that, use [those helper scripts](https://github.com/eirslett/frontend-maven-plugin/tree/master/frontend-maven-plugin/src/it/example%20project/helper-scripts)!

## To build this project:

Run `$ mvn clean install`

## Issues, Contributing

Please post any issues on the [Github's Issue tracker](https://github.com/eirslett/frontend-maven-plugin/issues). 
[Pull requests](https://github.com/eirslett/frontend-maven-plugin/pulls) are welcome! 
You can find a full list of [contributors here](https://github.com/eirslett/frontend-maven-plugin/graphs/contributors).

## License

[Apache 2.0](LICENSE)
