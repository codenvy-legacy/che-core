This repository contains core components of Che.

## How to build

Requirements: Maven 3.1.1+, Java 1.7 (JDK)
```
mvn clean install
```

## What's inside?

#### che-core-test-framework

Framework used to test plugins.

#### che-core-vfs-impl

Implementation of VirtualFileSystemProvider for a plain file system.

#### commons

Che commons classes used by all Che components and sub-modules.

#### ide

The skeleton of an IDE as a web application that includes UI components, client side API, editors abstractions etc.

#### platform-api-client-gwt

Clients for platform API (server side REST services).

#### platform-api

Che API, including models and REST services.
