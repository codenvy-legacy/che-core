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
import org.eclipse.che.api.machine.server.model.impl.ChannelsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineStateImpl;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentStateImpl;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeWorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
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
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceManager.class);

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
     *         when any error occurs
     */
    public List<UsersWorkspaceImpl> getWorkspaces(String owner) throws ServerException, BadRequestException {
        requiredNotNull(owner, "Workspace owner required");

        //Get list of runtime workspace, it is needed for usersWorkspaces status
        final Map<String, UsersWorkspace> runtimeWorkspaces = new HashMap<>();
        for (RuntimeWorkspace runtimeWorkspace : workspaceRegistry.getByOwner(owner)) {
            runtimeWorkspaces.put(runtimeWorkspace.getId(), runtimeWorkspace);
        }

        //Get list of users workspaces + set statuses
        final List<UsersWorkspaceImpl> usersWorkspaces = workspaceDao.getByOwner(owner);
        for (UsersWorkspaceImpl usersWorkspace : usersWorkspaces) {
            if (runtimeWorkspaces.containsKey(usersWorkspace.getId())) {
                usersWorkspace.setStatus(runtimeWorkspaces.get(usersWorkspace.getId()).getStatus());
            } else {
                usersWorkspace.setStatus(STOPPED);
            }
            usersWorkspace.setTemporary(false);
        }
        return usersWorkspaces;
    }

    /**
     * Gets runtime workspaces owned by specified user.
     *
     * @param owner
     *         workspaces owner identifier
     * @return list of runtime workspaces owned by {@code owner} or empty list when user doesn't have workspaces running
     */
    public List<RuntimeWorkspaceImpl> getRuntimeWorkspaces(String owner) throws BadRequestException {
        requiredNotNull(owner, "Workspace owner required");

        return workspaceRegistry.getByOwner(owner);
    }

    /**
     * Starts certain workspace with specified environment and account.
     * <p/>
     * <p>Workspace start is asynchronous
     *
     * @param workspaceId
     *         identifier of workspace which should be started
     * @param envName
     *         name of environment or null, when default environment should be used
     * @param accountId
     *         account which should be used for this runtime workspace or null when
     *         it should be automatically detected
     * @return starting workspace
     * @throws BadRequestException
     *         when {@code workspaceId} is null
     * @throws NotFoundException
     *         when workspace with given {@code workspaceId} doesn't exist, or
     *         {@link WorkspaceHooks#beforeStart(UsersWorkspace, String)} throws this exception
     * @throws ForbiddenException
     *         when user doesn't have access to start workspace in certain account
     * @throws ServerException
     *         when any other error occurs during workspace start
     * @see WorkspaceHooks#beforeStart(UsersWorkspace, String)
     * @see RuntimeWorkspaceRegistry#start(UsersWorkspace, String)
     */
    public UsersWorkspaceImpl startWorkspaceById(String workspaceId,
                                                 String envName,
                                                 String accountId)
            throws NotFoundException, ServerException, BadRequestException, ForbiddenException {
        requiredNotNull(workspaceId, "Workspace id required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceId);
        return startWorkspace(workspace, envName, accountId);
    }

    /**
     * Starts certain workspace with specified environment and account.
     * <p/>
     * <p>Workspace start is asynchronous
     *
     * @param workspaceName
     *         name of workspace which should be started
     * @param envName
     *         name of environment or null, when default environment should be used
     * @param owner
     *         owner of the workspace which should be started
     * @param accountId
     *         account which should be used for this runtime workspace or null when
     *         it should be automatically detected
     * @return starting workspace
     * @throws BadRequestException
     *         when given {@code workspaceName} or {@code owner} is null
     * @throws NotFoundException
     *         when workspace with given {@code workspaceName & owner} doesn't exist, or
     *         {@link WorkspaceHooks#beforeStart(UsersWorkspace, String)} throws this exception
     * @throws ForbiddenException
     *         when user doesn't have access to start workspace in certain account
     * @throws ServerException
     *         when any other error occurs during workspace start
     * @see WorkspaceHooks#beforeStart(UsersWorkspace, String)
     * @see RuntimeWorkspaceRegistry#start(UsersWorkspace, String)
     */
    public UsersWorkspaceImpl startWorkspaceByName(String workspaceName,
                                                   String envName,
                                                   String owner,
                                                   String accountId)
            throws NotFoundException, ServerException, BadRequestException, ForbiddenException {
        requiredNotNull(workspaceName, "Workspace name required");
        requiredNotNull(owner, "Workspace owner required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceName, owner);
        return startWorkspace(workspace, envName, accountId);
    }

    // TODO should we store temp workspaces and where?
    // should it be sync or async?

    /**
     * Starts temporary workspace based on config and account.
     * <p/>
     * <p>Workspace start is synchronous
     *
     * @param workspaceConfig
     *         workspace configuration
     * @param accountId
     *         account which should be used for this runtime workspace or null when
     *         it should be automatically detected
     * @return running workspace
     * @throws BadRequestException
     *         when {@code workspaceConfig} is null or not valid
     * @throws ForbiddenException
     *         when user doesn't have access to start workspace in certain account
     * @throws NotFoundException
     *         when {@link WorkspaceHooks#beforeCreate(UsersWorkspace, String)}
     *         or {@link WorkspaceHooks#beforeStart(UsersWorkspace, String)} throws this exception
     * @throws ServerException
     *         when any other error occurs during workspace start
     * @see WorkspaceHooks#beforeStart(UsersWorkspace, String)
     * @see WorkspaceHooks#beforeCreate(UsersWorkspace, String)
     * @see RuntimeWorkspaceRegistry#start(UsersWorkspace, String)
     */
    public RuntimeWorkspaceImpl startTemporaryWorkspace(WorkspaceConfig workspaceConfig, String accountId)
            throws ServerException, BadRequestException, ForbiddenException, NotFoundException, ConflictException {
        final UsersWorkspaceImpl workspace = fromConfig(workspaceConfig);
        workspace.setTemporary(true);

        hooks.beforeCreate(workspace, accountId);
        hooks.beforeStart(workspace, accountId);

        final RuntimeWorkspaceImpl runtimeWorkspace = startWorkspaceSync(workspace, null);

        // TODO when this code should be called for temp workspaces
        hooks.afterCreate(runtimeWorkspace, accountId);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}# TEMP#true#",
                 runtimeWorkspace.getName(),
                 runtimeWorkspace.getId(),
                 EnvironmentContext.getCurrent().getUser().getId());

        return runtimeWorkspace;
    }

    public void stopWorkspace(String workspaceId) throws ServerException, NotFoundException, ForbiddenException, BadRequestException {
        requiredNotNull(workspaceId, "Workspace id required");

        stopWorkspaceAsync(workspaceId);
    }

    public UsersWorkspaceImpl createWorkspace(WorkspaceConfig workspaceConfig, String owner, String accountId)
            throws NotFoundException, ForbiddenException, ServerException, BadRequestException, ConflictException {

        final UsersWorkspaceImpl workspace = fromConfig(workspaceConfig);
        workspace.setOwner(owner);

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

    public UsersWorkspaceImpl updateWorkspace(String workspaceId, WorkspaceConfig update)
            throws ConflictException, ServerException, BadRequestException, NotFoundException {
        validateName(update.getName());
        validateConfig(update);

        final UsersWorkspaceImpl oldWorkspace = workspaceDao.get(workspaceId);
        final UsersWorkspaceImpl updated = workspaceDao.update(new UsersWorkspaceImpl(update,
                                                                                      oldWorkspace.getId(),
                                                                                      oldWorkspace.getOwner()));
        try {
            updated.setStatus(workspaceRegistry.get(workspaceId).getStatus());
        } catch (NotFoundException ignored) {
            updated.setStatus(STOPPED);
        }

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

    /** ****************** */

    private UsersWorkspaceImpl startWorkspace(UsersWorkspaceImpl workspace,
                                              String envName,
                                              String accountId)
            throws ServerException, NotFoundException, ForbiddenException {

        workspace.setTemporary(false);
        hooks.beforeStart(workspace, accountId);
        startWorkspaceAsync(workspace, envName);
        workspace.setStatus(WorkspaceStatus.STARTING);
        return workspace;
    }

    void startWorkspaceAsync(final UsersWorkspaceImpl usersWorkspace, final String envName) {
        executor.execute(ThreadLocalPropagateContext.wrap(() -> {
            try {
                startWorkspaceSync(usersWorkspace, envName);
            } catch (BadRequestException | ServerException | NotFoundException | ConflictException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }));
    }

    RuntimeWorkspaceImpl startWorkspaceSync(final UsersWorkspaceImpl usersWorkspace, final String envName)
            throws BadRequestException, ServerException, NotFoundException, ConflictException {
        eventService.publish(newDto(WorkspaceStatusEvent.class)
                                     .withEventType(STARTING)
                                     .withWorkspaceId(usersWorkspace.getId()));

        try {
            final RuntimeWorkspaceImpl runtimeWorkspace = workspaceRegistry.start(usersWorkspace, envName);

            eventService.publish(newDto(WorkspaceStatusEvent.class)
                                         .withEventType(RUNNING)
                                         .withWorkspaceId(runtimeWorkspace.getId()));

            return runtimeWorkspace;

        } catch (ServerException | ConflictException | BadRequestException | NotFoundException e) {
            eventService.publish(newDto(WorkspaceStatusEvent.class)
                                         .withEventType(ERROR)
                                         .withWorkspaceId(usersWorkspace.getId())
                                         .withError(e.getLocalizedMessage()));

            throw e;
        }
    }

    void stopWorkspaceAsync(String workspaceId) {
        eventService.publish(newDto(WorkspaceStatusEvent.class)
                                     .withEventType(STOPPING)
                                     .withWorkspaceId(workspaceId));
        executor.execute(() -> {
            try {
                workspaceRegistry.stop(workspaceId);
            } catch (ConflictException | NotFoundException | ServerException e) {
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

        for (EnvironmentStateImpl environment : workspace.getEnvironments().values()) {
            for (MachineStateImpl machineState : environment.getMachineConfigs()) {
                machineState.setChannels(createMachineChannels(machineState.getName(), workspace.getId(), environment.getName()));
            }
        }

        return workspace;
    }

    private ChannelsImpl createMachineChannels(String machineName, String workspaceId, String envName) {
        return new ChannelsImpl(workspaceId + ':' + envName + ':' + machineName,
                                "machine:status:" + workspaceId + ':' + machineName);
    }

    /**
     * Checks that {@link WorkspaceConfig cfg} contains valid values, if it is not throws {@link BadRequestException}.
     * <p/>
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
