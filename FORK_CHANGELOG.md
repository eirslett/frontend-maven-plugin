# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed

- [DCA11Y-1274]: Null arguments for mojos would fail the build
- [DCA11Y-1274]: Fix incremental with Yarn berry by fixing runtime detection

### Added

- [DCA11Y-1274]: ".flattened-pom.xml" to the excluded filenames list after finding it in Jira

## [1.15.1-atlassian-2]

### Added

- [DCA11Y-1274]: Incremental builds for Yarn, Corepack and NPM goals 

## [1.15.1-atlassian-1]

- [DCA11Y-1145]: Automatic version detection of the Node version from `.tool-versions`, `.node-version`, and `.nvmrc` files
- [DCA11Y-1145]: The configuration property `nodeVersionFile` to specify a file that can be read in `install-node-and-npm`, `install-node-and-pnpm`, and `install-node-and-yarn`

### Changed

- [DCA11Y-1145]: Now tolerant of `v` missing or present at the start of a Node version

[DCA11Y-1274]: https://hello.jira.atlassian.cloud/browse/DCA11Y-1274
[DCA11Y-1145]: https://hello.jira.atlassian.cloud/browse/DCA11Y-1145
[unreleased]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.15.1-atlassian-2...HEAD
