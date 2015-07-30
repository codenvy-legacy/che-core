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
package org.eclipse.che.api.workspace.server;

import com.google.inject.Inject;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

import static org.eclipse.che.commons.lang.NameGenerator.generate;

/**
 * Facade for Workspace related operations
 *
 * @author gazarenkov
 */
@Singleton
public class WorkspaceManager {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    /* should contain [3, 20] characters, first and last character is letter or digit, available characters {A-Za-z0-9.-_}*/
    private static final Pattern WS_NAME = Pattern.compile("[\\w][\\w\\.\\-]{1,18}[\\w]");

    private final WorkspaceDao      workspaceDao;
    private final WorkspaceRegistry workspaceRegistry;
    private WorkspaceHooks hooks = null;

    @Inject
    public WorkspaceManager(WorkspaceDao workspaceDao, WorkspaceRegistry workspaceRegistry) {
        this.workspaceDao = workspaceDao;
        this.workspaceRegistry = workspaceRegistry;
    }

    @Inject(optional=true)
    public void setHooks(WorkspaceHooks hooks) {
        this.hooks = hooks;
    }

    public UsersWorkspace createWorkspace(final WorkspaceConfig workspaceConfig, final String accountId)
            throws NotFoundException, ConflictException, ServerException, BadRequestException {

        requiredNotNull(workspaceConfig, "Workspace config");

        validateName(workspaceConfig.getName());
        if (workspaceConfig.getAttributes() != null) {
            validateAttributes(workspaceConfig.getAttributes());
        }

        final UsersWorkspaceImpl workspace = UsersWorkspaceImpl.from(workspaceConfig);

        if (workspace.getName() == null || workspace.getName().isEmpty()) {
            workspace.setName(generateWorkspaceName());
        }

        if(hooks != null)
            hooks.beforeCreate(workspaceConfig, accountId);

        UsersWorkspace newWorkspace = workspaceDao.create(workspace);

        if(hooks != null)
            hooks.afterCreate(workspaceConfig, accountId);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", workspace.getName(), workspace.getId(),
                 EnvironmentContext.getCurrent().getUser().getId());

        return newWorkspace;
    }

    public UsersWorkspace updateWorkspace(String workspaceId, final WorkspaceConfig workspace)
            throws ConflictException, ServerException, BadRequestException, NotFoundException {

        requiredNotNull(workspace, "Workspace config");

        validateName(workspace.getName());
        if (workspace.getAttributes() != null) {
            validateAttributes(workspace.getAttributes());
        }

        final UsersWorkspace currentWorkspace = workspaceDao.get(workspaceId);
        UsersWorkspace newWorkspace = UsersWorkspaceImpl.from(workspace)
                                                       .setId(currentWorkspace.getId())
                                                       .setOwner(currentWorkspace.getOwner());

        UsersWorkspace updated = workspaceDao.update(newWorkspace);

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", updated.getName(), updated.getId());

        return updated;

    }

    public void removeWorkspace(String workspaceId) throws ConflictException, ServerException, NotFoundException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id");

        try {
            workspaceRegistry.get(workspaceId);

            throw new ConflictException("Can't remove running workspace " + workspaceId);
        } catch (NotFoundException e) {
            workspaceDao.remove(workspaceId);
            if(hooks != null)
                hooks.afterRemove(workspaceId);
            LOG.info("EVENT#workspace-remove# WS-ID#{}#", workspaceId);
        }
    }

    public UsersWorkspace getWorkspace(String workspaceId) throws NotFoundException, ServerException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id");

        return workspaceDao.get(workspaceId);
    }

    public UsersWorkspace getWorkspace(String workspaceName, String owner)
            throws NotFoundException, ServerException, BadRequestException {

        requiredNotNull(workspaceName, "Workspace name");

        requiredNotNull(owner, "Workspace owner");

        return workspaceDao.get(workspaceName, owner);
    }

    public RuntimeWorkspace startWorkspace(String workspaceId)
            throws NotFoundException, ServerException, ForbiddenException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id");

        final UsersWorkspace usersWorkspace = workspaceDao.get(workspaceId);

        return workspaceRegistry.start(usersWorkspace);
    }

    public void stopWorkspace(String workspaceId) throws ServerException, NotFoundException, ForbiddenException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id");

        workspaceRegistry.stop(workspaceId);
    }

    public RuntimeWorkspace getRuntimeWorkspace(String workspaceId) throws ServerException, BadRequestException, NotFoundException {
        requiredNotNull(workspaceId, "Workspace id");

        return workspaceRegistry.get(workspaceId);
    }

    public RuntimeWorkspace startTempWorkspace(WorkspaceConfig workspaceConfig, String owner)
            throws ServerException, NotFoundException, ForbiddenException, BadRequestException {
        requiredNotNull(workspaceConfig, "Workspace config");
        requiredNotNull(owner, "Workspace owner");

        return workspaceRegistry.start(UsersWorkspaceImpl.from(workspaceConfig)
                                                         .setOwner(owner)
                                                         .setId(generateWorkspaceId()));
    }

    /*******************************/

    private void validateName(String workspaceName) throws ConflictException {
        if (workspaceName == null) {
            throw new ConflictException("Workspace name required");
        }
        if (!WS_NAME.matcher(workspaceName).matches()) {
            throw new ConflictException("Incorrect workspace name, it should be between 3 to 20 characters and may contain digits, " +
                                        "latin letters, underscores, dots, dashes and should start and end only with digits, " +
                                        "latin letters or underscores");
        }
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws org.eclipse.che.api.core.BadRequestException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(subject + " required");
        }
    }

    /**
     * Validates attribute name.
     *
     * @param attributeName
     *         attribute name to check
     * @throws org.eclipse.che.api.core.ConflictException
     *         when attribute name is {@code null}, empty or it starts with "codenvy"
     */
    // TODO rename restricted attribute suffix to 'che:'
    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(String.format("Attribute name '%s' is not valid", attributeName));
        }
    }

    private void validateAttributes(Map<String, String> attributes) throws ConflictException {
        for (String attributeName : attributes.keySet()) {
            validateAttributeName(attributeName);
        }
    }

    /**
     * Generates workspace name based on current user email.
     * Generating process is simple, assuming we have user with email user@codenvy.com,
     * then first time we will check for workspace with name equal to "user" and if it is free
     * it will be returned, but if it is reserved then  number suffix will be added to the end of "user" name
     * and it will be checked again until free workspace name is not found.
     */
    private String generateWorkspaceName() throws ServerException {
        //should be email
        String userName = currentUser().getName();
        int atIdx = userName.indexOf('@');
        //if username contains email then fetch part before '@'
        if (atIdx != -1) {
            userName = userName.substring(0, atIdx);
        }
        //search first workspace name which is free
        int suffix = 2;
        String workspaceName = userName;
        while (workspaceExists(workspaceName)) {
            workspaceName = userName + suffix++;
        }
        return workspaceName;
    }

    private String generateWorkspaceId() {
        return generate("workspace", Constants.ID_LENGTH);
    }

    private boolean workspaceExists(String name) throws ServerException {
        try {
            workspaceDao.get(name);
        } catch (NotFoundException nfEx) {
            return false;
        }
        return true;
    }

    private org.eclipse.che.commons.user.User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }
}
