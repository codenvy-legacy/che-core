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

import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder configuration for particular build process.
 *
 * @author andrew00x
 */
public class BuilderConfiguration {
    private final java.io.File       buildDir;
    private final java.io.File       workDir;
    private final BuilderTaskType    taskType;
    private final BaseBuilderRequest request;

    public BuilderConfiguration(java.io.File buildDir, java.io.File workDir, BuilderTaskType taskType, BaseBuilderRequest request) {
        this.buildDir = buildDir;
        this.workDir = workDir;
        this.taskType = taskType;
        this.request = request;
    }

    public java.io.File getWorkDir() {
        return workDir;
    }

    public java.io.File getBuildDir() {
        return buildDir;
    }

    public BuilderTaskType getTaskType() {
        return taskType;
    }

    public List<String> getTargets() {
        return new ArrayList<>(request.getTargets());
    }

    public Map<String, String> getOptions() {
        return new LinkedHashMap<>(request.getOptions());
    }

    public BaseBuilderRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    @Override
    public String toString() {
        return "BuilderConfiguration{" +
               "workDir=" + workDir +
               ", taskType=" + taskType +
               ", request=" + request +
               '}';
    }
}
