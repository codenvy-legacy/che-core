# Eclipse Che Core
[![Join the chat at https://gitter.im/eclipse/che](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/eclipse/che?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/codenvy/che-core.svg?branch=master)](https://travis-ci.org/codenvy/che-core) 

[![License](https://img.shields.io/github/license/codenvy/che-core.svg)](https://github.com/codenvy/che-core)
[![Latest tag](https://img.shields.io/github/tag/codenvy/che-core.svg)](https://github.com/codenvy/che-core/tags)

Requirements: Maven 3.1.1+, Java 1.8 (JDK)
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
