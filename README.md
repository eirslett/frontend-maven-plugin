# Frontend maven plugin
This plugin downloads/installs Node and NPM locally for your project, runs NPM install, Grunt and/or Karma.
It's supposed to work on Windows, OS X and Linux.

# Installing
Include the plugin as a dependency in your Maven project.
`<plugins>
  <plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>0.0.2-SNAPSHOT</version>
  </plugin>
  ...`
Have a look at the example project, to see how it should be set up!

## To build this project:
mvn clean install

## Issues, Contributing
Please post any issues on the Github's Issue tracker. Pull requests are welcome!

### License
Apache 2.0