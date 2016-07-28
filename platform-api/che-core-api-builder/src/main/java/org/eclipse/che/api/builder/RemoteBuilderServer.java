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
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.HttpJsonRequest;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpJsonResponse;
import org.eclipse.che.api.core.rest.RemoteServiceDescriptor;
import org.eclipse.che.api.core.rest.shared.dto.Link;

import static org.eclipse.che.api.builder.BuilderUtils.builderRequest;

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

    public RemoteBuilderServer(String baseUrl, HttpJsonRequestFactory requestFactory) {
        super(baseUrl, requestFactory);
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
            return new RemoteBuilder(baseUrl, descriptor, getLinks(), requestFactory);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    public List<BuilderDescriptor> getBuilderDescriptors() throws BuilderException {
        return linkReq(Constants.LINK_REL_AVAILABLE_BUILDERS, 0).asList(BuilderDescriptor.class);
    }

    public ServerState getServerState() throws BuilderException {
        return linkReq(Constants.LINK_REL_SERVER_STATE, 10000).asDto(ServerState.class);
    }

    private HttpJsonResponse linkReq(String linkName, int timeout) throws BuilderException {
        final Link link;
        try {
            link = getLink(linkName);
            if (link == null) {
                throw new BuilderException("Unable get URL for '" + linkName + "'");
            }
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException e) {
            throw new BuilderException(e.getServiceError());
        }
        return builderRequest(requestFactory.fromLink(link).setTimeout(timeout));
    }

}
