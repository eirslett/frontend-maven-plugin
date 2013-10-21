[![Build Status](https://travis-ci.org/eirslett/frontend-maven-plugin.png?branch=master)](https://travis-ci.org/eirslett/frontend-maven-plugin)

# Frontend maven plugin
This plugin downloads/installs Node and NPM locally for your project, runs NPM install, Grunt and/or Karma.
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
[Here is an example for you!](https://github.com/eirslett/frontend-maven-plugin/tree/master/src/it/example)

# Installing
Include the plugin as a dependency in your Maven project.
```<plugins>
  <plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>0.0.6</version>
  </plugin>
```
Have a look at the example project, to see how it should be set up!

## To build this project:
mvn clean install

## Issues, Contributing
Please post any issues on the Github's Issue tracker. Pull requests are welcome!

### License
Apache 2.0
