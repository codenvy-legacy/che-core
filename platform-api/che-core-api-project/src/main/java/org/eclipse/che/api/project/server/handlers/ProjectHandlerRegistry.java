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
package org.eclipse.che.api.project.server.handlers;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectHandlerRegistry {

    private final Map<String, CreateProjectHandler>      createProjectHandlers      = new HashMap<>();
    private final Map<String, PostImportProjectHandler>  postImportProjectHandlers  = new HashMap<>();
    private final Map<String, GetItemHandler>            getItemHandlers            = new HashMap<>();
    private final Map<String, CreateModuleHandler>       createModuleHandlers       = new HashMap<>();
    private final Map<String, RemoveModuleHandler>       removeModuleHandlers       = new HashMap<>();
    private final Map<String, ProjectTypeChangedHandler> projectTypeChangedHandlers = new HashMap<>();
    private final Map<String, GetModulesHandler>         getModulesHandlers         = new HashMap<>();

    @Inject
    public ProjectHandlerRegistry(Set<ProjectHandler> projectHandlers) {
        for (ProjectHandler handler : projectHandlers) {
            register(handler);
        }
    }

    public void register(@NotNull ProjectHandler handler) {
        if (handler instanceof CreateProjectHandler) {
            createProjectHandlers.put(handler.getProjectType(), (CreateProjectHandler)handler);
        } else if (handler instanceof GetItemHandler) {
            getItemHandlers.put(handler.getProjectType(), (GetItemHandler)handler);
        } else if (handler instanceof CreateModuleHandler) {
            createModuleHandlers.put(handler.getProjectType(), (CreateModuleHandler)handler);
        } else if (handler instanceof RemoveModuleHandler) {
                removeModuleHandlers.put(handler.getProjectType(), (RemoveModuleHandler)handler);
        } else if (handler instanceof PostImportProjectHandler) {
            postImportProjectHandlers.put(handler.getProjectType(), (PostImportProjectHandler)handler);
        } else if (handler instanceof ProjectTypeChangedHandler) {
            projectTypeChangedHandlers.put(handler.getProjectType(), (ProjectTypeChangedHandler)handler);
        } else if (handler instanceof GetModulesHandler) {
            getModulesHandlers.put(handler.getProjectType(), (GetModulesHandler)handler);
        }
    }

    @Nullable
    public CreateProjectHandler getCreateProjectHandler(@NotNull String projectType) {
        return createProjectHandlers.get(projectType);
    }

    @Nullable
    public GetItemHandler getGetItemHandler(@NotNull String projectType) {
        return getItemHandlers.get(projectType);
    }

    @Nullable
    public CreateModuleHandler getCreateModuleHandler(@NotNull String projectType) {
        return createModuleHandlers.get(projectType);
    }

    @Nullable
    public RemoveModuleHandler getRemoveModuleHandler(@NotNull String projectType) {
        return removeModuleHandlers.get(projectType);
    }

    @Nullable
    public PostImportProjectHandler getPostImportProjectHandler(@NotNull String projectType) {
        return postImportProjectHandlers.get(projectType);
    }

    @Nullable
    public ProjectTypeChangedHandler getProjectTypeChangedHandler(@NotNull String projectType) {
        return projectTypeChangedHandlers.get(projectType);
    }

    @Nullable
    public GetModulesHandler getModulesHandler(@NotNull String projectType) {
        return getModulesHandlers.get(projectType);
    }

}
