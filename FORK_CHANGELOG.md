# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.15.1-atlassian-3]  - 2024-11-29

### Fixed

- [DCA11Y-1274]: Null arguments for mojos would fail the build
- [DCA11Y-1274]: Fix incremental with Yarn berry by fixing runtime detection
- [DCA11Y-1274]: Corepack Mojo incremental works
- [DCA11Y-1274]: Fix updating of digest versions without clean install
- [DCA11Y-1274]: Download dev metrics now correctly report PAC
- [DCA11Y-1145]: Fixed the legacy "downloadRoot" argument for PNPM & NPM installation

### Added

- [DCA11Y-1274]: ".flattened-pom.xml" & ".git" to the excluded filenames list after finding it in Jira
- [DCA11Y-1274]: Log message indicating how much time is saved

## [1.15.1-atlassian-2] - 2024-11-26

### Added

- [DCA11Y-1274]: Incremental builds for Yarn, Corepack and NPM goals 

## [1.15.1-atlassian-1] - 2024-11-25

- [DCA11Y-1145]: Automatic version detection of the Node version from `.tool-versions`, `.node-version`, and `.nvmrc` files
- [DCA11Y-1145]: The configuration property `nodeVersionFile` to specify a file that can be read in `install-node-and-npm`, `install-node-and-pnpm`, and `install-node-and-yarn`

### Changed

- [DCA11Y-1145]: Now tolerant of `v` missing or present at the start of a Node version



[DCA11Y-1274]: https://hello.jira.atlassian.cloud/browse/DCA11Y-1274
[DCA11Y-1145]: https://hello.jira.atlassian.cloud/browse/DCA11Y-1145

[unreleased]: https://github.com/atlassian-forks/frontend-maven-plugin/compare/frontend-plugins-1.15.1-atlassian-3...HEAD
[1.15.1-atlassian-3]: https://github.com/atlassian-forks/frontend-maven-plugin/compare/frontend-plugins-1.15.1-atlassian-2...frontend-plugins-1.15.1-atlassian-3
[1.15.1-atlassian-2]: https://github.com/atlassian-forks/frontend-maven-plugin/compare/frontend-plugins-1.15.1-atlassian-1...frontend-plugins-1.15.1-atlassian-2
[1.15.1-atlassian-1]: https://github.com/atlassian-forks/frontend-maven-plugin/compare/frontend-plugins-1.15.1-atlassian-1-16519678...frontend-plugins-1.15.1-atlassian-1
[1.15.1-atlassian-1-16519678]: https://github.com/atlassian-forks/frontend-maven-plugin/compare/frontend-plugins-1.15.1...frontend-plugins-1.15.1-atlassian-1-16519678
