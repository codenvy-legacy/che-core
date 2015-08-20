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

package org.eclipse.che.ide.bootstrap;

import org.eclipse.che.api.project.gwt.client.ProjectTypeServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class ProjectTypeComponent implements Component {
    private final ProjectTypeServiceClient projectTypeService;
    private final ProjectTypeRegistry projectTypeRegistry;
    private final DtoUnmarshallerFactory   dtoUnmarshallerFactory;

    @Inject
    public ProjectTypeComponent(ProjectTypeServiceClient projectTypeService,
                                ProjectTypeRegistry projectTypeRegistry, DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.projectTypeService = projectTypeService;
        this.projectTypeRegistry = projectTypeRegistry;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        projectTypeService.getProjectTypes(
                new AsyncRequestCallback<List<ProjectTypeDefinition>>(
                        dtoUnmarshallerFactory.newListUnmarshaller(ProjectTypeDefinition.class)) {

                    @Override
                    protected void onSuccess(List<ProjectTypeDefinition> result) {
                        for (ProjectTypeDefinition projectType : result) {
                            projectTypeRegistry.register(projectType);
                        }
                        callback.onSuccess(ProjectTypeComponent.this);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(new Exception("Can't load project types", exception));
                    }
                });
    }
}
