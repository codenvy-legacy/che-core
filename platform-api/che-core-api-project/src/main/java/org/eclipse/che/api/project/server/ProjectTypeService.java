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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;

import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedList;
import java.util.List;

/**
 * ProjectTypeService
 *
 * @author gazarenkov
 */
@Path("project-type")
public class ProjectTypeService extends Service {

    private ProjectTypeRegistry registry;

    @Inject
    public ProjectTypeService(ProjectTypeRegistry registry) {
        this.registry = registry;
    }

    @GenerateLink(rel = Constants.LINK_REL_PROJECT_TYPES)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTypeDefinition> getProjectTypes() {
        final List<ProjectTypeDefinition> types = new LinkedList<>();
        for (ProjectType type : registry.getProjectTypes()) {
            types.add(DtoConverter.toTypeDescriptor2(type));
        }
        return types;
    }
}