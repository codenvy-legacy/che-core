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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Provide information about registered ProjectTemplates via REST.
 *
 * @author Vitaly Parfonov
 */
@Path("project-template/{ws-id}")
public class ProjectTemplateService extends Service {

    private ProjectTemplateRegistry templateRegistry;

    @Inject
    public ProjectTemplateService(ProjectTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    @GET
    @Path("{projectType}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTemplateDescriptor> getProjectTemplates(@PathParam("projectType") String projectType) {
        return templateRegistry.getTemplates(projectType);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectTemplateDescriptor> getProjectTemplates() {
        return templateRegistry.getAllTemplates();
    }
}
