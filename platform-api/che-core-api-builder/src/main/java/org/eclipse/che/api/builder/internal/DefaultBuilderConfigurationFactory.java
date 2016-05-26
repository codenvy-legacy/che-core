/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.builder.BuilderException;
import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.api.builder.dto.BuildRequest;
import org.eclipse.che.api.builder.dto.DependencyRequest;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Base implementation of BuilderConfigurationFactory.
 *
 * @author andrew00x
 */
public class DefaultBuilderConfigurationFactory implements BuilderConfigurationFactory {
    private final Builder builder;

    public DefaultBuilderConfigurationFactory(Builder builder) {
        this.builder = builder;
    }

    @Override
    public BuilderConfiguration createBuilderConfiguration(BaseBuilderRequest request) throws BuilderException {
        if (request instanceof BuildRequest) {
            final java.io.File buildDir = createBuildDir();
            return new BuilderConfiguration(buildDir, createWorkDir(buildDir, request), BuilderTaskType.DEFAULT, request);
        } else if (request instanceof DependencyRequest) {
            final DependencyRequest myRequest = (DependencyRequest)request;
            String type = myRequest.getType();
            if (type == null) {
                type = "list";
            }
            final BuilderTaskType taskType;
            switch (type) {
                case "copy":
                    taskType = BuilderTaskType.COPY_DEPS;
                    break;
                case "list":
                    taskType = BuilderTaskType.LIST_DEPS;
                    break;
                default:
                    throw new BuilderException(
                            String.format("Unsupported type of an analysis task: %s. Should be either 'list' or 'copy'", type));
            }
            final java.io.File buildDir = createBuildDir();
            return new BuilderConfiguration(buildDir, createWorkDir(buildDir, request), taskType, myRequest);
        }
        throw new BuilderException("Unsupported type of request");
    }

    protected java.io.File createBuildDir() throws BuilderException {
        try {
            return Files.createTempDirectory(builder.getBuildDirectory().toPath(), "build-").toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }

    /**
     * Work directory that will be created matches build-<generated number>/project-name.
     *
     * @param request
     *         the request for this new build
     * @return the folder that will be used as work directory
     * @throws BuilderException
     *         if there is any exception (like creating the directories)
     * @see #createBuildDir()
     */
    protected java.io.File createWorkDir(java.io.File parent, BaseBuilderRequest request) throws BuilderException {
        try {
            return Files.createDirectory(new java.io.File(parent, request.getProjectDescriptor().getName()).toPath()).toFile();
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }
}
