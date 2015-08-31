/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server.handlers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public void register(@Nonnull ProjectHandler handler) {
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
    public CreateProjectHandler getCreateProjectHandler(@Nonnull String projectType) {
        return createProjectHandlers.get(projectType);
    }

    @Nullable
    public GetItemHandler getGetItemHandler(@Nonnull String projectType) {
        return getItemHandlers.get(projectType);
    }

    @Nullable
    public CreateModuleHandler getCreateModuleHandler(@Nonnull String projectType) {
        return createModuleHandlers.get(projectType);
    }

    @Nullable
    public RemoveModuleHandler getRemoveModuleHandler(@Nonnull String projectType) {
        return removeModuleHandlers.get(projectType);
    }

    @Nullable
    public PostImportProjectHandler getPostImportProjectHandler(@Nonnull String projectType) {
        return postImportProjectHandlers.get(projectType);
    }

    @Nullable
    public ProjectTypeChangedHandler getProjectTypeChangedHandler(@Nonnull String projectType) {
        return projectTypeChangedHandlers.get(projectType);
    }

    @Nullable
    public GetModulesHandler getModulesHandler(@Nonnull String projectType) {
        return getModulesHandlers.get(projectType);
    }

}
