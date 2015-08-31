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

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeWorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPED;
import static org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent.EventType.ERROR;
import static org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent.EventType.RUNNING;
import static org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent.EventType.STARTING;
import static org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent.EventType.STOPPING;
import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

//TODO document it

/**
 * Facade for Workspace related operations
 *
 * @author gazarenkov
 * @author Alexander Garagatyi
 */
@Singleton
public class WorkspaceManager {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    /* should contain [3, 20] characters, first and last character is letter or digit, available characters {A-Za-z0-9.-_}*/
    private static final Pattern WS_NAME = Pattern.compile("[\\w][\\w\\.\\-]{1,18}[\\w]");

    private final WorkspaceDao             workspaceDao;
    private final RuntimeWorkspaceRegistry workspaceRegistry;
    private final EventService             eventService;
    private final ExecutorService          executor;

    private WorkspaceHooks hooks = WorkspaceHooks.NOOP_WORKSPACE_HOOKS;

    @Inject
    public WorkspaceManager(WorkspaceDao workspaceDao, RuntimeWorkspaceRegistry workspaceRegistry, EventService eventService) {
        this.workspaceDao = workspaceDao;
        this.workspaceRegistry = workspaceRegistry;
        this.eventService = eventService;

        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WorkspaceManager-%d")
                                                                           .setDaemon(true)
                                                                           .build());
    }

    @Inject(optional = true)
    public void setHooks(WorkspaceHooks hooks) {
        this.hooks = hooks;
    }

    /**
     * Get all user workspaces of specific user
     *
     * @param owner
     *         id of the owner of workspace
     * @return list of {@link UsersWorkspace}
     * @throws ServerException
     *         if any error occurs
     */
    public List<UsersWorkspace> getWorkspaces(String owner) throws ServerException, BadRequestException {
        requiredNotNull(owner, "Workspace owner required");

        final List<RuntimeWorkspaceImpl> runtimeWorkspaces = workspaceRegistry.getByOwner(owner);
        final List<UsersWorkspaceImpl> usersWorkspaces = workspaceDao.getList(owner);
        Map<String, UsersWorkspace> workspaces = new HashMap<>();
        for (RuntimeWorkspace runtimeWorkspace : runtimeWorkspaces) {
            workspaces.put(runtimeWorkspace.getId(), runtimeWorkspace);
        }
        for (UsersWorkspaceImpl usersWorkspace : usersWorkspaces) {
            if (!workspaces.containsKey(usersWorkspace.getId())) {
                usersWorkspace.setTemporary(false);
                usersWorkspace.setStatus(STOPPED);
                workspaces.put(usersWorkspace.getId(), usersWorkspace);
            }
        }

        return new ArrayList<>(workspaces.values());
    }

    public UsersWorkspace startWorkspaceById(String workspaceId, String envName)
            throws NotFoundException, ServerException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceId);
        workspace.setTemporary(false);
        startWorkspaceAsync(workspace, envName);
        workspace.setStatus(WorkspaceStatus.STARTING);
        return workspace;
    }

    public UsersWorkspaceImpl startWorkspaceByName(String workspaceName, String envName, String owner)
            throws NotFoundException, ServerException, BadRequestException {
        requiredNotNull(workspaceName, "Workspace name required");
        requiredNotNull(owner, "Workspace owner required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceName, owner);
        workspace.setTemporary(false);
        startWorkspaceAsync(workspace, envName);
        workspace.setStatus(WorkspaceStatus.STARTING);
        return workspace;
    }

    // TODO should we store temp workspaces and where?
    // should it be sync or async?
    public UsersWorkspaceImpl startTemporaryWorkspace(WorkspaceConfig workspaceConfig, final String accountId)
            throws ServerException, BadRequestException, ForbiddenException, NotFoundException {
        final UsersWorkspaceImpl workspace = fromConfig(workspaceConfig);
        workspace.setTemporary(true);

        hooks.beforeCreate(workspace, accountId);

        startWorkspaceSync(workspace, null);

        // TODO when this code should be called for temp workspaces
        hooks.afterCreate(workspace, accountId);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}# TEMP#true#",
                 workspace.getName(),
                 workspace.getId(),
                 EnvironmentContext.getCurrent().getUser().getId());

        workspace.setStatus(WorkspaceStatus.RUNNING);
        return workspace;
    }

    public void stopWorkspace(String workspaceId) throws ServerException, NotFoundException, ForbiddenException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id required");

        stopWorkspaceAsync(workspaceId);
    }

    public UsersWorkspaceImpl createWorkspace(final WorkspaceConfig workspaceConfig, final String accountId)
            throws NotFoundException, ForbiddenException, ServerException, BadRequestException, ConflictException {

        final UsersWorkspaceImpl workspace = fromConfig(workspaceConfig);

        hooks.beforeCreate(workspace, accountId);

        final UsersWorkspaceImpl newWorkspace = workspaceDao.create(workspace);
        newWorkspace.setStatus(STOPPED);

        hooks.afterCreate(workspace, accountId);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#",
                 workspace.getName(),
                 workspace.getId(),
                 getCurrentUserId());

        return newWorkspace;
    }

    public UsersWorkspace updateWorkspace(String workspaceId, WorkspaceConfig update)
            throws ConflictException, ServerException, BadRequestException, NotFoundException {
        validateName(update.getName());
        validateConfig(update);

        final UsersWorkspaceImpl oldWorkspace = workspaceDao.get(workspaceId);
        final UsersWorkspaceImpl updated = workspaceDao.update(new UsersWorkspaceImpl(update,
                                                                                      oldWorkspace.getId(),
                                                                                      oldWorkspace.getOwner()));

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", updated.getName(), updated.getId());

        return updated;
    }

    public void removeWorkspace(String workspaceId) throws ConflictException, ServerException, NotFoundException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id required");

        if (workspaceRegistry.isRunning(workspaceId)) {
            throw new ConflictException("Can't remove running workspace " + workspaceId);
        }

        workspaceDao.remove(workspaceId);

        hooks.afterRemove(workspaceId);

        LOG.info("EVENT#workspace-remove# WS-ID#{}#", workspaceId);
    }

    public UsersWorkspaceImpl getWorkspace(String workspaceId) throws NotFoundException, ServerException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceId);
        try {
            final RuntimeWorkspaceImpl runtimeWorkspace = workspaceRegistry.get(workspaceId);
            workspace.setStatus(runtimeWorkspace.getStatus());
        } catch (NotFoundException ignored) {
            //if registry doesn't contain workspace it should have stopped status
            workspace.setStatus(STOPPED);
        }
        return workspace;
    }

    public RuntimeWorkspaceImpl getRuntimeWorkspace(String workspaceId) throws BadRequestException, NotFoundException, ServerException {
        requiredNotNull(workspaceId, "Workspace id required");

        return workspaceRegistry.get(workspaceId);
    }

    //TODO
    public RuntimeWorkspace getRuntimeWorkspace(String name, String owner) {
        return null;
    }

    public UsersWorkspace getWorkspace(String name, String owner) throws BadRequestException, NotFoundException, ServerException {
        requiredNotNull(name, "Workspace name required");
        requiredNotNull(owner, "Workspace owner required");

        return workspaceDao.get(name, owner);
    }

    /*******************************/

    void startWorkspaceAsync(final UsersWorkspaceImpl usersWorkspace, final String envName) {
        executor.execute(ThreadLocalPropagateContext.wrap(new Runnable() {
            @Override
            public void run() {
                startWorkspaceSync(usersWorkspace, envName);
            }
        }));
    }

    void startWorkspaceSync(final UsersWorkspaceImpl usersWorkspace, final String envName) {
        eventService.publish(newDto(WorkspaceStatusEvent.class)
                                     .withEventType(STARTING)
                                     .withWorkspaceId(usersWorkspace.getId()));

        try {
            workspaceRegistry.start(usersWorkspace, envName);

            eventService.publish(newDto(WorkspaceStatusEvent.class)
                                         .withEventType(RUNNING)
                                         .withWorkspaceId(usersWorkspace.getId()));

        } catch (ServerException | ConflictException | BadRequestException | NotFoundException e) {
            eventService.publish(newDto(WorkspaceStatusEvent.class)
                                         .withEventType(ERROR)
                                         .withWorkspaceId(usersWorkspace.getId())
                                         .withError(e.getLocalizedMessage()));

            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    void stopWorkspaceAsync(String workspaceId) {
        eventService.publish(newDto(WorkspaceStatusEvent.class)
                                     .withEventType(STOPPING)
                                     .withWorkspaceId(workspaceId));
        executor.execute(() -> {
            try {
                workspaceRegistry.stop(workspaceId);
            } catch (ForbiddenException | NotFoundException | ServerException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

            eventService.publish(newDto(WorkspaceStatusEvent.class)
                                         .withEventType(WorkspaceStatusEvent.EventType.STOPPED)
                                         .withWorkspaceId(workspaceId));
        });
    }

    UsersWorkspaceImpl fromConfig(final WorkspaceConfig cfg) throws BadRequestException, ForbiddenException, ServerException {
        requiredNotNull(cfg, "Workspace config required");
        validateConfig(cfg);

        final UsersWorkspaceImpl workspace = new UsersWorkspaceImpl(cfg, generateWorkspaceId(), getCurrentUserId());

        if (Strings.isNullOrEmpty(workspace.getName())) {
            workspace.setName(generateWorkspaceName());
        } else {
            validateName(cfg.getName());
        }

        //set websocket output channels
        for (EnvironmentImpl environment : workspace.getEnvironments().values()) {
            for (MachineConfigImpl machineConfig : environment.getMachineConfigs()) {
                machineConfig.setOutputChannel(workspace.getId() + ':' + environment.getName() + ':' + machineConfig.getName());
            }
        }

        return workspace;
    }

    /**
     * Checks that {@link WorkspaceConfig cfg} contains valid values, if it is not throws {@link BadRequestException}.
     *
     * Validation rules:
     * <ul>
     * <li>{@link WorkspaceConfig#getDefaultEnvName()} must not be empty or null</li>
     * <li>{@link WorkspaceConfig#getEnvironments()} must contain {@link WorkspaceConfig#getDefaultEnvName() default environment}
     * which is declared in the same configuration</li>
     * <li>{@link Environment#getName()} must not be null</li>
     * <li>{@link Environment#getMachineConfigs()} must contain at least 1 machine(which is dev),
     * also it must contain exactly one dev machine</li>
     * </ul>
     */
    private void validateConfig(WorkspaceConfig cfg) throws BadRequestException {
        //attributes
        for (String attributeName : cfg.getAttributes().keySet()) {
            //attribute name should not be empty and should not start with codenvy
            if (attributeName.trim().isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
                throw new BadRequestException(format("Attribute name '%s' is not valid", attributeName));
            }
        }

        //environments
        requiredNotNull(cfg.getDefaultEnvName(), "Workspace default environment name required");
        requiredNotNull(cfg.getEnvironments().get(cfg.getDefaultEnvName()), "Workspace default environment configuration required");
        for (Environment environment : cfg.getEnvironments().values()) {
            final String envName = environment.getName();
            requiredNotNull(envName, "Environment name should not be null");

            //machine configs
            if (environment.getMachineConfigs().isEmpty()) {
                throw new BadRequestException("Environment '" + envName + "' should contain at least 1 machine");
            }
            final long devCount = environment.getMachineConfigs()
                                             .stream()
                                             .filter(MachineConfig::isDev)
                                             .count();
            if (devCount != 1) {
                throw new BadRequestException(format("Environment should contain exactly 1 dev machine, but '%s' contains '%d'",
                                                     envName,
                                                     devCount));
            }
            for (MachineConfig machineCfg : environment.getMachineConfigs()) {
                if (isNullOrEmpty(machineCfg.getName())) {
                    throw new BadRequestException("Environment " + envName + " contains machine without of name");
                }
                requiredNotNull(machineCfg.getSource(), "Environment " + envName + " contains machine without of source");
                //TODO require type?
            }
        }

        //commands
        for (Command command : cfg.getCommands()) {
            requiredNotNull(command.getName(), "Workspace " + cfg.getName() + " contains command without of name");
            requiredNotNull(command.getCommandLine(), format("Command line required for command '%s' in workspace '%s'",
                                                             command.getName(),
                                                             cfg.getName()));
        }

        //projects
        //TODO
    }

    private void validateName(String workspaceName) throws BadRequestException {
        if (isNullOrEmpty(workspaceName)) {
            throw new BadRequestException("Workspace name should not be null or empty");
        }
        if (!WS_NAME.matcher(workspaceName).matches()) {
            throw new BadRequestException("Incorrect workspace name, it should be between 3 to 20 characters and may contain digits, " +
                                          "latin letters, underscores, dots, dashes and should start and end only with digits, " +
                                          "latin letters or underscores");
        }
    }

    private String getCurrentUserId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param message
     *         used as subject of exception message "{subject} required"
     * @throws org.eclipse.che.api.core.BadRequestException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String message) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(message);
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
        String userName = EnvironmentContext.getCurrent().getUser().getName();
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

}
