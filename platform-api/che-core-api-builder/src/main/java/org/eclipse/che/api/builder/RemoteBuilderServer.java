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
package org.eclipse.che.api.builder;

import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.builder.dto.ServerState;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.RemoteServiceDescriptor;
import org.eclipse.che.api.core.rest.shared.dto.Link;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory for RemoteBuilder. See {@link RemoteBuilder} about usage of this class.
 *
 * @author andrew00x
 * @see RemoteBuilder
 */
public class RemoteBuilderServer extends RemoteServiceDescriptor {

    /** Name of IDE workspace this server used for. */
    private String assignedWorkspace;
    /** Name of project inside IDE workspace this server used for. */
    private String assignedProject;

    public RemoteBuilderServer(String baseUrl) {
        super(baseUrl);
    }

    public String getAssignedWorkspace() {
        return assignedWorkspace;
    }

    public void setAssignedWorkspace(String assignedWorkspace) {
        this.assignedWorkspace = assignedWorkspace;
    }

    public String getAssignedProject() {
        return assignedProject;
    }

    public void setAssignedProject(String assignedProject) {
        this.assignedProject = assignedProject;
    }

    public boolean isDedicated() {
        return assignedWorkspace != null;
    }

    public RemoteBuilder getRemoteBuilder(String name) throws BuilderException {
        for (BuilderDescriptor builderDescriptor : getBuilderDescriptors()) {
            if (name.equals(builderDescriptor.getName())) {
                return createRemoteBuilder(builderDescriptor);
            }
        }
        throw new BuilderException(String.format("Invalid builder name %s", name));
    }

    public List<RemoteBuilder> getRemoteBuilders() throws BuilderException {
        List<RemoteBuilder> remoteBuilders = new LinkedList<>();
        for (BuilderDescriptor builderDescriptor : getBuilderDescriptors()) {
            remoteBuilders.add(createRemoteBuilder(builderDescriptor));
        }
        return remoteBuilders;
    }

    private RemoteBuilder createRemoteBuilder(BuilderDescriptor descriptor) throws BuilderException {
        try {
            return new RemoteBuilder(baseUrl, descriptor, getLinks());
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    public List<BuilderDescriptor> getBuilderDescriptors() throws BuilderException {
        try {
            final Link link = getLink(Constants.LINK_REL_AVAILABLE_BUILDERS);
            if (link == null) {
                throw new BuilderException("Unable get URL for retrieving list of remote builders");
            }
            return HttpJsonHelper.requestArray(BuilderDescriptor.class, link);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    public ServerState getServerState() throws BuilderException {
        try {
            final Link stateLink = getLink(Constants.LINK_REL_SERVER_STATE);
            if (stateLink == null) {
                throw new BuilderException(String.format("Unable get URL for getting state of a remote server '%s'", baseUrl));
            }
            return HttpJsonHelper.request(ServerState.class, 10000, stateLink);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }
}
