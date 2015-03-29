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
package org.eclipse.che.api.runner;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.RemoteServiceDescriptor;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceDescriptor;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.dto.RunnerServerDescriptor;
import org.eclipse.che.api.runner.dto.ServerState;
import org.eclipse.che.api.runner.internal.Constants;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory for RemoteRunner. See {@link RemoteRunner} about usage of this class.
 *
 * @author andrew00x
 */
public class RemoteRunnerServer extends RemoteServiceDescriptor {
    /** Name of IDE workspace this server is used for. */
    private String assignedWorkspace;
    /** Name of project inside IDE workspace this server is used for. */
    private String assignedProject;

    private String infra = "community";

    public RemoteRunnerServer(String baseUrl) {
        super(baseUrl);
    }

    public String getInfra() {
        return infra;
    }

    public void setInfra(String infra) {
        this.infra = infra;
    }

    public String getAssignedWorkspace() throws RunnerException {
        if (assignedWorkspace == null) {
            try {
                return ((RunnerServerDescriptor)getServiceDescriptor()).getAssignedWorkspace();
            } catch (IOException e) {
                throw new RunnerException(e);
            } catch (ServerException e) {
                throw new RunnerException(e.getServiceError());
            }
        }
        return assignedWorkspace;
    }

    public void setAssignedWorkspace(String assignedWorkspace) {
        this.assignedWorkspace = assignedWorkspace;
    }

    public String getAssignedProject() throws RunnerException {
        if (assignedProject == null) {
            try {
                return ((RunnerServerDescriptor)getServiceDescriptor()).getAssignedProject();
            } catch (IOException e) {
                throw new RunnerException(e);
            } catch (ServerException e) {
                throw new RunnerException(e.getServiceError());
            }
        }
        return assignedProject;
    }

    public void setAssignedProject(String assignedProject) {
        this.assignedProject = assignedProject;
    }

    public boolean isDedicated() throws RunnerException {
        return getAssignedWorkspace() != null;
    }

    public RemoteRunner getRemoteRunner(String name) throws RunnerException {
        for (RunnerDescriptor runnerDescriptor : getRunnerDescriptors()) {
            if (name.equals(runnerDescriptor.getName())) {
                return createRemoteRunner(runnerDescriptor);
            }
        }
        throw new RunnerException(String.format("Invalid runner name %s", name));
    }

    public List<RemoteRunner> getRemoteRunners() throws RunnerException {
        List<RemoteRunner> remoteRunners = new LinkedList<>();
        for (RunnerDescriptor runnerDescriptor : getRunnerDescriptors()) {
            remoteRunners.add(createRemoteRunner(runnerDescriptor));
        }
        return remoteRunners;
    }

    private RemoteRunner createRemoteRunner(RunnerDescriptor descriptor) throws RunnerException {
        try {
            return new RemoteRunner(baseUrl, descriptor.getName(), getLinks());
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public List<RunnerDescriptor> getRunnerDescriptors() throws RunnerException {
        try {
            final Link link = getLink(Constants.LINK_REL_AVAILABLE_RUNNERS);
            if (link == null) {
                throw new RunnerException("Unable get URL for retrieving list of remote runners");
            }
            return HttpJsonHelper.requestArray(RunnerDescriptor.class, link);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public ServerState getServerState() throws RunnerException {
        try {
            final Link stateLink = getLink(Constants.LINK_REL_SERVER_STATE);
            if (stateLink == null) {
                throw new RunnerException(String.format("Unable get URL for getting state of a remote server '%s'", baseUrl));
            }
            return HttpJsonHelper.request(ServerState.class, 10000, stateLink);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    @Override
    protected Class<? extends ServiceDescriptor> getServiceDescriptorClass() {
        return RunnerServerDescriptor.class;
    }
}
