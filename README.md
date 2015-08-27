# Eclipse Che Core
[![Join the chat at https://gitter.im/eclipse/che](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/eclipse/che?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/codenvy/che-core.svg?branch=master)](https://travis-ci.org/codenvy/che-core) 

[![License](https://img.shields.io/github/license/codenvy/che-core.svg)](https://github.com/codenvy/che-core)
[![Latest tag](https://img.shields.io/github/tag/codenvy/che-core.svg)](https://github.com/codenvy/che-core/tags)


## About Eclipse Che
High performance, open source developer environments in the cloud.

* **che**:                     [Main assembly repo - start here] (https://github.com/codenvy/che)
* **che-plugins**:             [Language & tooling extensions] (http://github.com/codenvy/che-plugins)
* **che-depmgt**:              [Maven dependency management POM] (http://github.com/codenvy/che-depmgt)
* **che-parent**:              [Maven parent POM] (http://github.com/codenvy/che-parent)
* **cli**:                     [CLI for interacting with Che remotely] (http://github.com/codenvy/cli)
* **eclipse-plugin**:          [Eclipse IDE plug-in for Che projects] (http://github.com/codenvy/eclipse-plugin)

## About This Module
This module contains the platform API, virtual file system, testing framework, IDE skeleton, and commons. You need Java SDK 1.8 and maven 3.x to build this module.

## License
Che is open sourced under the Eclipse Public License 1.0.

## Clone
```sh
git clone https://github.com/codenvy/che-core.git
```
If master is unstable, checkout the latest tagged version.

## Build
```sh
cd che-core
mvn clean install
```

## What's Inside?

#### che-core-test-framework
Framework used to test plugins.

#### che-core-vfs-impl
Implementation of VirtualFileSystemProvider for a plain file system.

#### commons
Commons classes used by components and sub-modules.

#### ide
The skeleton of an IDE as a web application that includes UI components, client side API, editors abstractions, wizards, panels, debugger, etc.

#### platform-api-client-gwt
Clients for platform API (server side REST services).

#### platform-api
Che API, including models and REST services.
