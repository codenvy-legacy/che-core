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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.MoreObjects.firstNonNull;
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

    private final WorkspaceDao             workspaceDao;
    private final RuntimeWorkspaceRegistry workspaceRegistry;
    private       WorkspaceConfigValidator workspaceConfigValidator;
    private final EventService eventService;
    private final ExecutorService          executor;
    private final MachineManager           machineManager;

    private WorkspaceHooks hooks = WorkspaceHooks.NOOP_WORKSPACE_HOOKS;

    @Inject
    public WorkspaceManager(WorkspaceDao workspaceDao,
                            RuntimeWorkspaceRegistry workspaceRegistry,
                            WorkspaceConfigValidator workspaceConfigValidator,
                            EventService eventService,
                            MachineManager machineManager) {
        this.workspaceDao = workspaceDao;
        this.workspaceRegistry = workspaceRegistry;
        this.workspaceConfigValidator = workspaceConfigValidator;
        this.eventService = eventService;
        this.machineManager = machineManager;

        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WorkspaceManager-%d")
                                                                           .setDaemon(true)
                                                                           .build());
    }

    @Inject(optional = true)
    public void setHooks(WorkspaceHooks hooks) {
        this.hooks = hooks;
    }

    /**
     * Gets all workspaces belonging to given user.
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

            addChannels(usersWorkspace);
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

        List<RuntimeWorkspaceImpl> workspaces = workspaceRegistry.getByOwner(owner);

        workspaces.forEach(this::addChannels);

        return workspaces;
    }

    /**
     * Starts certain workspace with specified environment and account.
     *
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
     *         {@link WorkspaceHooks#beforeStart(UsersWorkspace, String, String)} throws this exception
     * @throws ForbiddenException
     *         when user doesn't have access to start workspace in certain account
     * @throws ServerException
     *         when any other error occurs during workspace start
     * @see WorkspaceHooks#beforeStart(UsersWorkspace, String, String)
     * @see RuntimeWorkspaceRegistry#start(UsersWorkspace, String)
     */
    public UsersWorkspaceImpl startWorkspaceById(String workspaceId,
                                                 String envName,
                                                 String accountId)
            throws NotFoundException, ServerException, BadRequestException, ForbiddenException, ConflictException {
        requiredNotNull(workspaceId, "Workspace id required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceId);
        return startWorkspace(workspace, envName, accountId, false);
    }

    public UsersWorkspaceImpl recoverWorkspace(String workspaceId, String envName, String accountId)
            throws BadRequestException, NotFoundException, ServerException, ConflictException, ForbiddenException {
        requiredNotNull(workspaceId, "Workspace id required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceId);
        return startWorkspace(workspace, envName, accountId, true);
    }

    /**
     * Creates snapshot of runtime workspace.
     *
     * <p>Basically creates {@link SnapshotImpl snapshot} instance for each machine from
     * runtime workspace's active environment.
     *
     * <p> Note that:
     * <br>Snapshots are created asynchronously
     * <br>If snapshot creation for one machine failed, it wouldn't affect another snapshot creations
     *
     * @param workspaceId
     *         runtime workspace id
     * @return list of created snapshots
     * @throws BadRequestException
     *         when workspace id is null
     * @throws NotFoundException
     *         when runtime workspace with given id does not exist
     * @throws ServerException
     *         when any other error occurs
     */
    public List<SnapshotImpl>createSnapshot(String workspaceId) throws BadRequestException, NotFoundException, ServerException {
        requiredNotNull(workspaceId, "Required non-null workspace id");

        final RuntimeWorkspaceImpl workspace = workspaceRegistry.get(workspaceId);
        final List<SnapshotImpl> snapshots = new ArrayList<>(workspace.getMachines().size());
        for (MachineStateImpl machineState : workspace.getMachines()) {
            try {
                snapshots.add(machineManager.save(machineState.getId(), workspace.getOwner(), workspace.getActiveEnvName()));
            } catch (ApiException apiEx) {
                LOG.error(apiEx.getLocalizedMessage(), apiEx);
            }
        }
        return snapshots;
    }

    /**
     * Starts certain workspace with specified environment and account.
     *
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
     *         {@link WorkspaceHooks#beforeStart(UsersWorkspace, String, String)} throws this exception
     * @throws ForbiddenException
     *         when user doesn't have access to start workspace in certain account
     * @throws ServerException
     *         when any other error occurs during workspace start
     * @see WorkspaceHooks#beforeStart(UsersWorkspace, String, String)
     * @see RuntimeWorkspaceRegistry#start(UsersWorkspace, String)
     */
    public UsersWorkspaceImpl startWorkspaceByName(String workspaceName,
                                                   String envName,
                                                   String owner,
                                                   String accountId)
            throws NotFoundException, ServerException, BadRequestException, ForbiddenException, ConflictException {
        requiredNotNull(workspaceName, "Workspace name required");
        requiredNotNull(owner, "Workspace owner required");

        final UsersWorkspaceImpl workspace = workspaceDao.get(workspaceName, owner);
        return startWorkspace(workspace, envName, accountId, false);
    }

    // TODO should we store temp workspaces and where?
    // should it be sync or async?

    /**
     * Starts temporary workspace based on config and account.
     *
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
     *         or {@link WorkspaceHooks#beforeStart(UsersWorkspace, String, String)} throws this exception
     * @throws ServerException
     *         when any other error occurs during workspace start
     * @see WorkspaceHooks#beforeStart(UsersWorkspace, String, String)
     * @see WorkspaceHooks#beforeCreate(UsersWorkspace, String)
     * @see RuntimeWorkspaceRegistry#start(UsersWorkspace, String)
     */
    public RuntimeWorkspaceImpl startTemporaryWorkspace(WorkspaceConfig workspaceConfig, String accountId)
            throws ServerException, BadRequestException, ForbiddenException, NotFoundException, ConflictException {
        final UsersWorkspaceImpl workspace = fromConfig(workspaceConfig);
        workspace.setTemporary(true);

        hooks.beforeCreate(workspace, accountId);
        hooks.beforeStart(workspace, workspace.getDefaultEnvName(), accountId);

        final RuntimeWorkspaceImpl runtimeWorkspace = startWorkspaceSync(workspace, workspace.getDefaultEnvName(), false);

        addChannels(runtimeWorkspace);

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

        addChannels(workspace);

        hooks.afterCreate(workspace, accountId);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#",
                 workspace.getName(),
                 workspace.getId(),
                 getCurrentUserId());

        return newWorkspace;
    }

    private void addChannels(UsersWorkspaceImpl workspace) {
        for (EnvironmentStateImpl environment : workspace.getEnvironments().values()) {
            for (MachineStateImpl machineState : environment.getMachineConfigs()) {
                machineState.setChannels(MachineManager.createMachineChannels(machineState.getName(),
                                                                              workspace.getId(),
                                                                              environment.getName()));
            }
        }
    }

    public UsersWorkspaceImpl updateWorkspace(String workspaceId, WorkspaceConfig update)
            throws ConflictException, ServerException, BadRequestException, NotFoundException {
        workspaceConfigValidator.validate(update);

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

            addChannels(runtimeWorkspace);

            workspace.setStatus(runtimeWorkspace.getStatus());
        } catch (NotFoundException ignored) {
            //if registry doesn't contain workspace it should have stopped status
            workspace.setStatus(STOPPED);
        }
        return workspace;
    }

    public RuntimeWorkspaceImpl getRuntimeWorkspace(String workspaceId) throws BadRequestException, NotFoundException, ServerException {
        requiredNotNull(workspaceId, "Workspace id required");

        RuntimeWorkspaceImpl workspace = workspaceRegistry.get(workspaceId);

        addChannels(workspace);

        return workspace;
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

    private UsersWorkspaceImpl startWorkspace(UsersWorkspaceImpl workspace, String envName, String accountId, boolean recover)
            throws ServerException, NotFoundException, ForbiddenException, ConflictException {
        envName = firstNonNull(envName, workspace.getDefaultEnvName());

        try {
            final RuntimeWorkspaceImpl runtime = workspaceRegistry.get(workspace.getId());
            throw new ConflictException(format("Could not start workspace '%s' because its status is '%s'",
                                               runtime.getName(),
                                               runtime.getStatus()));
        } catch (NotFoundException ignored) {
            //it is okay if workspace does not exist
        }

        workspace.setTemporary(false);
        hooks.beforeStart(workspace, envName, accountId);
        startWorkspaceAsync(workspace, envName, recover);
        workspace.setStatus(WorkspaceStatus.STARTING);
        return workspace;
    }

    UsersWorkspaceImpl startWorkspaceAsync(UsersWorkspaceImpl usersWorkspace, String envName, boolean recover) {
        executor.execute(ThreadLocalPropagateContext.wrap(() -> {
            try {
                startWorkspaceSync(usersWorkspace, envName, recover);
            } catch (BadRequestException | ServerException | NotFoundException | ConflictException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }));

        addChannels(usersWorkspace);

        return usersWorkspace;
    }

    RuntimeWorkspaceImpl startWorkspaceSync(final UsersWorkspaceImpl usersWorkspace, final String envName, boolean recover)
            throws BadRequestException, ServerException, NotFoundException, ConflictException {
        eventService.publish(newDto(WorkspaceStatusEvent.class)
                                     .withEventType(STARTING)
                                     .withWorkspaceId(usersWorkspace.getId()));

        try {
            final RuntimeWorkspaceImpl runtimeWorkspace = workspaceRegistry.start(usersWorkspace, envName, recover);

            eventService.publish(newDto(WorkspaceStatusEvent.class)
                                         .withEventType(RUNNING)
                                         .withWorkspaceId(runtimeWorkspace.getId()));

            addChannels(usersWorkspace);

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
        workspaceConfigValidator.validateWithoutWorkspaceName(cfg);

        final UsersWorkspaceImpl workspace = new UsersWorkspaceImpl(cfg, generateWorkspaceId(), getCurrentUserId());

        if (Strings.isNullOrEmpty(workspace.getName())) {
            workspace.setName(generateWorkspaceName());
        } else {
            workspaceConfigValidator.validateWorkspaceName(cfg.getName());
        }

        return workspace;
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
