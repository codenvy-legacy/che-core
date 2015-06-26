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

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectTypeServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.core.Component;

import java.util.List;

/**
 * @author Evgen Vidolob
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectTypeComponent implements Component {

    private final ProjectTypeServiceClient projectTypeService;
    private final ProjectTypeRegistry      projectTypeRegistry;

    @Inject
    public ProjectTypeComponent(ProjectTypeServiceClient projectTypeService, ProjectTypeRegistry projectTypeRegistry) {
        this.projectTypeService = projectTypeService;
        this.projectTypeRegistry = projectTypeRegistry;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        projectTypeService.getProjectTypes().then(new Operation<List<ProjectTypeDefinition>>() {
            @Override
            public void apply(List<ProjectTypeDefinition> arg) throws OperationException {
                projectTypeRegistry.registerAll(arg);
                callback.onSuccess(ProjectTypeComponent.this);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                callback.onFailure(new Exception("Can't load project types: " + arg.toString()));
            }
        });
    }
}
