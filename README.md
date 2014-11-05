OS X Build: (Travis CI) [![Build Status](https://travis-ci.org/eirslett/frontend-maven-plugin.png?branch=master)](https://travis-ci.org/eirslett/frontend-maven-plugin)

Windows Build: (Appveyor) [![Build status](https://ci.appveyor.com/api/projects/status/vxbccc1t9ceadhi9)](https://ci.appveyor.com/project/eirslett/frontend-maven-plugin)

# Frontend maven plugin
This plugin downloads/installs Node and NPM locally for your project, runs NPM install, Grunt and/or gulp and/or Karma.
It's supposed to work on Windows, OS X and Linux.

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

## Show me an example!
[Here is an example for you!](https://github.com/eirslett/frontend-maven-plugin/tree/master/frontend-maven-plugin/src/it/example%20project)

# Installing
Include the plugin as a dependency in your Maven project.
```xml
<plugins>
    <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <version>0.0.16</version>
        ...
    </plugin>
...
```

# Usage
Have a look at the example project, to see how it should be set up!
https://github.com/eirslett/frontend-maven-plugin/blob/master/frontend-maven-plugin/src/it/example%20project/pom.xml

### Working directory
The working directory is where you've put `package.json` and your frontend configuration files (`Gruntfile.js` or `gulpfile.js` etc). The default working directory is your project's base directory (the same directory as your `pom.xml`). You can change the working directory if you want:
```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>...</version>
    
    <!-- optional -->
    <configuration>
        <workingDirectory>src/main/frontend</workingDirectory>
    </configuration>
    
    <executions>
      ...
    </executions>
</plugin>
```

### Installing node and npm
The versions of Node and npm are downloaded from http://nodejs.org/dist, extracted and put into a `node` folder created in your working directory. (Remember to gitignore the `node` folder, unless you actually want to commit it)
Node/npm will only be "installed" locally to your project. It will not be installed globally on the whole system (and it will not interfere with any Node/npm installations already present.)
```xml
<execution>
    <!-- optional: you don't really need execution ids,
    but it looks nice in your build log. -->
    <id>install node and npm</id>
    <goals>
        <goal>install-node-and-npm</goal>
    </goals>
    <!-- optional: default phase is "generate-resources" -->
    <phase>generate-resources</phase>
    <configuration>
        <nodeVersion>v0.10.18</nodeVersion>
        <npmVersion>1.3.8</npmVersion>
        <!-- optional: where to download node and npm from. Defaults to http://nodejs.org/dist/ -->
        <downloadRoot>http://myproxy.example.org/nodejs/dist/</downloadRoot>
    </configuration>
</execution>
```
__Proxy settings:__ If you need to download Node/npm through a proxy: [configure your Maven proxy settings](http://maven.apache.org/guides/mini/guide-proxies.html) in ~/.m2/settings.xml. This plugin will use the same proxy settings as Maven.


### Running npm
All npm modules will be installed in the `node_modules` folder in your working directory.
By default, no colors will be shown in the log.
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

### Running Grunt
It will run Grunt according to the `Gruntfile.js` in your working directory.
By default, no colors will be shown in the log.
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
By default, no colors will be shown in the log.
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
__Skipping tests:__ If you run maven with the `-DskipTests` flag, karma tests will be skipped.

__Ignoring failed tests:__ If you want to ignore test failures run maven with the `-DtestFailureIgnore` flag, karma test results will not stop the build but test results will remain
in test output files. Suitable for continuous integration tool builds.

__Why karma.conf.ci.js?__ When using Karma, you should have two separate
configurations: `karma.conf.js` and `karma.conf.ci.js`. (The second one should inherit configuration
from the first one, and override some options. The example project shows you how to set it up.)
The idea is that you use `karma.conf.js` while developing (using watch/livereload etc.), and
`karma.conf.ci.js` when building - for example, when building, it should only run karma once,
it should generate xml reports, it should run only in PhantomJS, and/or it should generate
code coverage reports.

__Running Karma through Grunt or gulp:__ You may choose to run Karma [directly through Grunt](https://github.com/karma-runner/grunt-karma) or [through gulp](https://github.com/karma-runner/gulp-karma) instead,
as part of the `grunt` or `gulp` execution. That will help to separate your frontend and backend builds even more.


# Eclipse M2E support

This plugin contains support for M2E, including lifecycle mappings and support for incremental builds in Eclipse. 
The `install-node-and-npm` goal will only run on a full project build. The other goals support incremental builds 
to avoid doing unnecessary work. During an incremental build the `npm` goal will only run if the `package.json` file 
has been changed. The `grunt` and `gulp` goals have new `srcdir` and `triggerfiles` optional configuration options; if
these are set they check for changes in your source files before being run. See the wiki for more information.

# Helper scripts
During development, it's convenient to have the "npm", "grunt", "gulp" and "karma" commands
available on the command line. If you want that, use [those helper scripts](https://github.com/eirslett/frontend-maven-plugin/tree/master/frontend-maven-plugin/src/it/example%20project/helper-scripts)!

## To build this project:
`mvn clean install`

## Issues, Contributing
Please post any issues on the Github's Issue tracker. Pull requests are welcome!

### License
Apache 2.0
