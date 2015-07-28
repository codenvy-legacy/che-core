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

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.UsersWorkspace;
import org.eclipse.che.api.core.model.WorkspaceConfig;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.dao.MembershipDao;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.workspace.server.event.AfterCreateWorkspaceEvent;
import org.eclipse.che.api.workspace.server.event.BeforeCreateWorkspaceEvent;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDo;
import org.eclipse.che.api.core.model.Workspace;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.singletonMap;

/**
 * Facade for Workspace related operations
 *
 * @author gazarenkov
 */
@Singleton
public class WorkspaceManager {

    public static final String WORKSPACE_SCOPE = "workspace";
    private static final Logger LOG = LoggerFactory.getLogger(NewWorkspaceService.class);

    /* should contain [3, 20] characters, first and last character is letter or digit, available characters {A-Za-z0-9.-_}*/
    private static final Pattern WS_NAME       = Pattern.compile("[\\w][\\w\\.\\-]{1,18}[\\w]");


    private final WorkspaceDao workspaceDao;
//    private final UserDao userDao;
//    private final UserProfileDao profileDao;
//    private final PreferenceDao preferenceDao;
    private final EventService eventService;
//    private final MembershipDao membershipDao;

    private final WorkspaceRuntimes workspaceRuntimes;

    @Inject
    public WorkspaceManager(WorkspaceDao workspaceDao, WorkspaceRuntimes workspaceRuntimes, EventService eventService) {

//    public WorkspaceManager(WorkspaceDao workspaceDao, UserDao userDao, UserProfileDao profileDao, PreferenceDao preferenceDao,
//                            final MembershipDao membershipDao, EventService eventService) {
        this.workspaceDao = workspaceDao;
//        this.userDao = userDao;
//        this.profileDao = profileDao;
//        this.preferenceDao = preferenceDao;
        this.eventService = eventService;
//        this.membershipDao = membershipDao;
        this.workspaceRuntimes = workspaceRuntimes;

    }

    public WorkspaceDo createWorkspace(final UsersWorkspace workspace, final String accountId) throws ConflictException, ServerException, BadRequestException {

        validateName(workspace.getName());

        requiredNotNull(workspace, "New workspace");

        if (workspace.getAttributes() != null) {
            validateAttributes(workspace.getAttributes());
        }
        if (workspace.getName() == null || workspace.getName().isEmpty()) {
            workspace.setName(generateWorkspaceName());
        }

        eventService.publish(new BeforeCreateWorkspaceEvent() {
            @Override
            public UsersWorkspace getWorkspace() {
                return workspace;
            }

            @Override
            public String getAccountId() {
                return accountId;
            }
        });


        WorkspaceDo newWorkspace = workspaceDao.create(workspace);

        eventService.publish(new AfterCreateWorkspaceEvent() {
            @Override
            public UsersWorkspace getWorkspace() {
                return workspace;
            }

            @Override
            public String getAccountId() {
                return accountId;
            }
        });

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", workspace.getName(), workspace.getId(),
                EnvironmentContext.getCurrent().getUser().getId());

        return newWorkspace;

    }


    public WorkspaceDo updateWorkspace(String id, final WorkspaceConfig workspace) throws ConflictException, ServerException,
            BadRequestException, NotFoundException {

        validateName(workspace.getName());

        requiredNotNull(workspace, "New workspace");

        if (workspace.getAttributes() != null) {
            validateAttributes(workspace.getAttributes());
        }


        final String newName = workspace.getName();
        if (newName != null) {
            workspace.setName(newName);
        }

        // TODO before?

        WorkspaceDo updated = workspaceDao.update(workspace);

        // TODO after?

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", updated.getName(), updated.getId());

        return updated;

    }

    public void removeWorkspace(String id) throws ConflictException, ServerException,
             NotFoundException {

        // TODO before?

        workspaceDao.remove(id);

        // TODO after?

        LOG.info("EVENT#workspace-remove# WS-ID#{}#",  id);


    }

    public WorkspaceDo getWorkspace(String workspaceId) throws NotFoundException, ServerException {

//        UsersWorkspace workspace = this.workspaceRuntimes.get(workspaceId);
//        if(workspace != null)
//            return workspace;
//        else

        // TODO before?
        return workspaceDao.get(workspaceId);

        // TODO after?



//        // tmp_workspace_cloned_from_private_repo - gives information
//        // whether workspace was clone from private repository or not. It can be use
//        // by temporary workspace sharing filter for user that are not workspace/admin
//        // so we need that property here.
//        // PLZ DO NOT REMOVE!!!!
//        final Map<String, String> attributes = workspace.getAttributes();
//        if (attributes.containsKey("allowAnyoneAddMember")) {
//            workspace.setAttributes(singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
//        } else {
//            attributes.clear();
//        }


    }


    public WorkspaceDo getWorkspace(String workspaceName, String owner) throws NotFoundException, ServerException,
            BadRequestException {

        requiredNotNull(workspaceName, "Workspace name");

        // TODO before?

        return workspaceDao.get(workspaceName, owner);


        // TODO after?


//        // tmp_workspace_cloned_from_private_repo - gives information
//        // whether workspace was clone from private repository or not. It can be use
//        // by temporary workspace sharing filter for user that are not workspace/admin
//        // so we need that property here.
//        // PLZ DO NOT REMOVE!!!!
//        final Map<String, String> attributes = workspace.getAttributes();
//        if (attributes.containsKey("allowAnyoneAddMember")) {
//            workspace.setAttributes(singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
//        } else {
//            attributes.clear();
//        }


    }


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
    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(String.format("Attribute2 name '%s' is not valid", attributeName));
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
