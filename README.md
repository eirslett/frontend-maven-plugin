# Atlassian fork of "frontend-maven-plugin"

[![license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) ![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)

## Intro

First read the upstream [README](https://github.com/eirslett/frontend-maven-plugin)

## Purpose

There are a few changes that are not wanted upstream, but are useful to developing for Atlassian products

## Goals

* Make it as easy as possible to use without subverting expectations
* Keep in sync with upstream
* Upstream as much as possible

## Issues, Contributing

Thank you for considering a contribution! Pull requests, issues and comments are welcome. 

Please try to contribute upstream first. It maximises the value out of every change and keeps this fork easier to maintain. You can check [upstream's issue tracker](https://github.com/eirslett/frontend-maven-plugin/issues) and [pull requests](https://github.com/eirslett/frontend-maven-plugin/pulls) to gauge maintainer opinion and avoid duplicate work.

Please minimise changes to the upstream code to keep syncing with upstream easy.

For pull requests, please:

* Add tests for new features and bug fixes
* Follow the existing style
* Separate unrelated changes into multiple pull requests

See the existing issues for things to start contributing.

For bigger changes, please make sure you start a discussion first by creating an issue and explaining the intended change.

Atlassian requires contributors to sign a Contributor License Agreement, known as a CLA. This serves as a record stating that the contributor is entitled to contribute the code/documentation/translation to the project and is willing to have it used in distributions and derivative works (or is willing to transfer ownership).

Prior to accepting your contributions we ask that you please follow the appropriate link below to digitally sign the CLA. The Corporate CLA is for those who are contributing as a member of an organization and the individual CLA is for those contributing as an individual.

* [CLA for corporate contributors](https://opensource.atlassian.com/corporate)
* [CLA for individuals](https://opensource.atlassian.com/individual)

### Releasing

Only Atlassians may release a new version, [follow this guide](https://hello.atlassian.net/wiki/spaces/~278062200/pages/1407390489/HOW+TO+Do+a+manual+maven+artifact+release).

## Usage requirements

* Java 8, until support is dropped from all DC products ([currently projected for 2026-04-19](https://hello.atlassian.net/wiki/spaces/DCCore/pages/3989804253/When+can+I+stop+supporting+Java+8+11+entirely+in+DC))
* Maven 3.6 (because of upstream)

## Usage guidance

### Format of the Node version

It shouldn't matter if the `v` prefix is present, e.g. `14.8.0` and `v14.8.0`.

Old, non-standard, and codename versions are also supported if they're [available](https://nodejs.org/dist), e.g. `latest-v12.x`.

### Using Node version files

The plugin should automatically detect the version from files like: `.node-version`, `.nvmrc`, and `.tool-versions`. Comments in the files should be ignored. If the file is not in the working directory, nor any of the parent directories, it can be manually set in the configuration like so:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <configuration>
        <nodeVersionFile>${project.basedir}/dotfiles/.nvmrc</nodeVersionFile>
    </configuration>
</plugin>
```

[![Cheers from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-cheers-light.png)](https://www.atlassian.com)
