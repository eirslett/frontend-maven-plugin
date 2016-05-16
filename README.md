OS X Build: (Travis CI) [![Build Status](https://travis-ci.org/eirslett/frontend-maven-plugin.png?branch=master)](https://travis-ci.org/eirslett/frontend-maven-plugin)

Windows Build: (Appveyor) [![Build status](https://ci.appveyor.com/api/projects/status/vxbccc1t9ceadhi9)](https://ci.appveyor.com/project/eirslett/frontend-maven-plugin)

Linux Build: (CloudBees) [![Build status](https://eirslett.ci.cloudbees.com/buildStatus/icon?job=Frontend%20maven%20plugin)](https://eirslett.ci.cloudbees.com/job/Frontend%20maven%20plugin/)

# Frontend maven plugin
This plugin downloads/installs Node and NPM locally for your project, runs NPM install, and then any combination of [Bower](http://bower.io/), [Grunt](http://gruntjs.com/), [Gulp](http://gulpjs.com/), [Jspm](http://jspm.io), [Karma](http://karma-runner.github.io/), or [Webpack](http://webpack.github.io/).
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
## Maven 3
```xml
<plugins>
    <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <!-- Use the latest released version:
        https://repo1.maven.org/maven2/com/github/eirslett/frontend-maven-plugin/ -->
        <version>1.0</version>
        ...
    </plugin>
...
```

For *Maven 2* support take a look at the [wiki](https://github.com/eirslett/frontend-maven-plugin/wiki#maven-2).

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

### Installation Directory
The installation directory is the folder where your dependencies are installed e.g. node.exe.
You can set this property on the different goals.
```xml
<execution>
    <id>npm install</id>
    <goals>
        <goal>npm</goal>
    </goals>
    <configuration>
        <arguments>install</arguments>
        <installDirectory>target</installDirectory>
    </configuration>
</execution>
```

Or choose to set it for all the goals, in the maven configuration.

```xml
<plugins>
    <plugin>
        <groupId>com.github.eirslett</groupId>
        <artifactId>frontend-maven-plugin</artifactId>
        <version>0.0.27</version>

        <configuration>
            <installDirectory>target</installDirectory>
        </configuration>
```


### Installing node and npm
The versions of Node and npm are downloaded from https://nodejs.org/dist, extracted and put into a `node` folder created in your working directory. (Remember to gitignore the `node` folder, unless you actually want to commit it)
Node/npm will only be "installed" locally to your project. It will not be installed globally on the whole system (and it will not interfere with any Node/npm installations already present.)
```xml
<plugin>
  ...
  <execution>
      <!-- optional: you don't really need execution ids,
      but it looks nice in your build log. -->
      <id>install node and npm</id>
      <goals>
          <goal>install-node-and-npm</goal>
      </goals>
      <!-- optional: default phase is "generate-resources" -->
      <phase>generate-resources</phase>
  </execution>
  <configuration>
      <nodeVersion>v0.10.18</nodeVersion>
      <npmVersion>1.3.8</npmVersion>
      <!-- optional: where to download node and npm from. Defaults to https://nodejs.org/dist/ -->
      <downloadRoot>http://myproxy.example.org/nodejs/dist/</downloadRoot>
      <!-- optional: where to install node and npm. Defaults to the working directory -->
      <installDirectory>target</installDirectory>
   </configuration>
</plugin>
```

You can also specify separate download roots for npm and node as they are now stored in separate repos.
```xml
<plugin>
  ...
  <execution>
      ...
  </execution>
  <configuration>
      <nodeVersion>v0.12.1</nodeVersion>
      <npmVersion>2.7.1</npmVersion>
      <nodeDownloadRoot>https://nodejs.org/dist/</nodeDownloadRoot>
      <npmDownloadRoot>https://registry.npmjs.org/npm/-/</npmDownloadRoot>
  </configuration>
</plugin>
```

### Proxy settings

If you have [configured proxy settings for Maven](http://maven.apache.org/guides/mini/guide-proxies.html)
in your settings.xml file, the plugin will automatically use the proxy for downloading node and npm, as well
as [passing the proxy to npm commands](https://docs.npmjs.com/misc/config#proxy).

__Non Proxy Hosts:__ npm does not currently support non proxy hosts - if you are using a proxy and npm install is 
is not downloading from your repository, it may be because it cannot be accessed through your proxy. 
If that is the case, you can stop the npm execution from inheriting the Maven proxy settings like this:
```xml
<execution>
    <id>npm install</id>
    <goals>
        <goal>npm</goal>
    </goals>
    <configuration>
        <npmInheritsProxyConfigFromMaven>false</npmInheritsProxyConfigFromMaven>
    </configuration>
</execution>
```

### Running npm
All npm modules will be installed in the `node_modules` folder in your working directory.
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

__Ignoring failed tests:__ If you want to ignore test failures run maven with the `-Dmaven.test.failure.ignore` flag, karma test results will not stop the build but test results will remain
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

# Eclipse M2E support

This plugin contains support for M2E, including lifecycle mappings and support for incremental builds in Eclipse.
The `install-node-and-npm` goal will only run on a full project build. The other goals support incremental builds
to avoid doing unnecessary work. During an incremental build the `npm` goal will only run if the `package.json` file
has been changed. The `grunt` and `gulp` goals have new `srcdir` and `triggerfiles` optional configuration options; if
these are set they check for changes in your source files before being run. See the wiki for more information.

# Helper scripts
During development, it's convenient to have the "npm", "bower", "grunt", "gulp" and "karma" commands
available on the command line. If you want that, use [those helper scripts](https://github.com/eirslett/frontend-maven-plugin/tree/master/frontend-maven-plugin/src/it/example%20project/helper-scripts)!

## To build this project:
`mvn clean install`

## Issues, Contributing
Please post any issues on the Github's Issue tracker. Pull requests are welcome!

### License
Apache 2.0
