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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.rest.DefaultHttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;

import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * For caching and proxy-ing Workspace Configuration
 *
 * @author gazarenkov
 */
public class WorkspaceHolder {

    private String apiEndpoint;

    private final UsersWorkspace workspace;

    private HttpJsonRequestFactory httpJsonRequestFactory;


    public WorkspaceHolder(@Named("api.endpoint") String apiEndpoint)
            throws ServerException {

        this.apiEndpoint = apiEndpoint;
        this.httpJsonRequestFactory = new DefaultHttpJsonRequestFactory();

        // TODO - invent mechanism to recognize workspace ID
        // for Docker container name of this property is defined in
        // org.eclipse.che.plugin.docker.machine.DockerInstanceMetadata.CHE_WORKSPACE_ID
        // it resides on Workspace Master side so not accessible from agent code
        String workspaceId = System.getProperty("CHE_WORKSPACE_ID");

        if (workspaceId == null)
            throw new ServerException("Workspace ID is not defined for Workspace Agent");

        this.workspace = getWorkspaceDto(workspaceId);
        //this.workspace = new UsersWorkspaceImpl(dto, dto.getId(), dto.getOwner());

    }

    protected WorkspaceHolder(UsersWorkspace workspace) {

        this.workspace = workspace;
    }

    /**
     * @return workspace object
     */
    public UsersWorkspace getWorkspace() {
        return this.workspace;
    }

    /**
     * @param wsId
     * @return
     * @throws ServerException
     */
    private UsersWorkspaceDto getWorkspaceDto(String wsId) throws ServerException {

        final String href = UriBuilder.fromUri(apiEndpoint)
                                      .path(WorkspaceService.class).path(WorkspaceService.class, "getById")
                                      .build(wsId).toString();
        final Link link = newDto(Link.class).withMethod("GET").withHref(href);

        try {
            return httpJsonRequestFactory.fromLink(link).request().asDto(UsersWorkspaceDto.class);
        } catch (IOException | ApiException e) {
            throw new ServerException(e);
        }
    }


}
