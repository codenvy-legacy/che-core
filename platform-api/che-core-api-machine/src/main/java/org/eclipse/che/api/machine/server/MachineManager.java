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
package org.eclipse.che.api.machine.server;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineState;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.FileLineConsumer;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.WebsocketLineConsumer;
import org.eclipse.che.api.machine.server.dao.SnapshotDao;
import org.eclipse.che.api.machine.server.exception.InvalidRecipeException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.exception.UnsupportedRecipeException;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.model.impl.ChannelsImpl;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineStateImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.shared.dto.event.MachineProcessEvent;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.che.api.machine.server.InstanceStateEvent.Type.DIE;
import static org.eclipse.che.api.machine.server.InstanceStateEvent.Type.OOM;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineManager {
    private static final Logger  LOG                          = LoggerFactory.getLogger(MachineManager.class);
    /* machine name must contain only {a-zA-Z0-9_-} characters and it's needed for validation machine names */
    private static final Pattern MACHINE_DISPLAY_NAME_PATTERN = Pattern.compile("^/?[a-zA-Z0-9_-]+$");

    private final SnapshotDao              snapshotDao;
    private final File                     machineLogsDir;
    private final MachineInstanceProviders machineInstanceProviders;
    private final ExecutorService          executor;
    private final MachineRegistry          machineRegistry;
    private final EventService             eventService;
    private final String                   apiEndpoint;
    private final int                      defaultMachineMemorySizeMB;
    private final MachineCleaner           machineCleaner;

    @Inject
    public MachineManager(SnapshotDao snapshotDao,
                          MachineRegistry machineRegistry,
                          MachineInstanceProviders machineInstanceProviders,
                          @Named("machine.logs.location") String machineLogsDir,
                          EventService eventService,
                          @Named("machine.default_mem_size_mb") int defaultMachineMemorySizeMB,
                          @Named("api.endpoint") String apiEndpoint) {
        this.snapshotDao = snapshotDao;
        this.machineInstanceProviders = machineInstanceProviders;
        this.eventService = eventService;
        this.apiEndpoint = apiEndpoint;
        this.machineLogsDir = new File(machineLogsDir);
        this.machineRegistry = machineRegistry;
        this.defaultMachineMemorySizeMB = defaultMachineMemorySizeMB;

        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("MachineManager-%d").setDaemon(true).build());
        this.machineCleaner = new MachineCleaner();
    }

    /**
     * Synchronously creates and starts machine from scratch using recipe.
     *
     * @param machineConfig
     *         configuration that contains all information needed for machine creation
     * @param workspaceId
     *         id of the workspace the created machine will belong to
     * @param environmentName
     *         environment name the created machine will belongs to
     * @return state of the new machine
     * @throws UnsupportedRecipeException
     *         if recipe isn't supported
     * @throws InvalidRecipeException
     *         if recipe is not valid
     * @throws NotFoundException
     *         if machine type from recipe is unsupported
     * @throws NotFoundException
     *         if snapshot not found
     * @throws NotFoundException
     *         if no instance provider implementation found for provided machine type
     * @throws SnapshotException
     *         if error occurs on retrieving snapshot information
     * @throws ConflictException
     *         if machine with given name already exists
     * @throws BadRequestException
     *         if machine display name is invalid
     * @throws MachineException
     *         if any other exception occurs during starting
     */
    public MachineImpl createMachineSync(MachineConfig machineConfig,
                                         final String workspaceId,
                                         final String environmentName)
            throws NotFoundException,
                   SnapshotException,
                   ConflictException,
                   MachineException,
                   BadRequestException {
        final MachineStateImpl machine = createMachine(machineConfig,
                                                       workspaceId,
                                                       environmentName,
                                                       this::createInstance);

        return instanceToMachineImpl(machineRegistry.get(machine.getId()));
    }

    /**
     * Asynchronously creates and starts machine from scratch using recipe.
     *
     * @param machineConfig
     *         configuration that contains all information needed for machine creation
     * @param workspaceId
     *         id of the workspace the created machine will belong to
     * @param environmentName
     *         environment name the created machine will belongs to
     * @return state of the new machine
     * @throws UnsupportedRecipeException
     *         if recipe isn't supported
     * @throws InvalidRecipeException
     *         if recipe is not valid
     * @throws NotFoundException
     *         if machine type from recipe is unsupported
     * @throws NotFoundException
     *         if snapshot not found
     * @throws NotFoundException
     *         if no instance provider implementation found for provided machine type
     * @throws SnapshotException
     *         if error occurs on retrieving snapshot information
     * @throws ConflictException
     *         if machine with given name already exists
     * @throws BadRequestException
     *         if machine display name is invalid
     * @throws MachineException
     *         if any other exception occurs during starting
     */
    public MachineStateImpl createMachineAsync(MachineConfig machineConfig,
                                               final String workspaceId,
                                               final String environmentName)
            throws NotFoundException,
                   SnapshotException,
                   ConflictException,
                   MachineException,
                   BadRequestException {
        return createMachine(machineConfig,
                             workspaceId,
                             environmentName,
                             (instanceProvider, recipe, instanceKey, machineState, machineLogger) ->
                                     executor.execute(ThreadLocalPropagateContext.wrap(() -> {
                                         try {
                                             createInstance(instanceProvider,
                                                            recipe,
                                                            instanceKey,
                                                            machineState,
                                                            machineLogger);
                                         } catch (MachineException | NotFoundException e) {
                                             LOG.error(e.getLocalizedMessage(), e);
                                             // todo what should we do in that case?
                                         }
                                     })));
    }

    private MachineStateImpl createMachine(MachineConfig machineConfig,
                                           final String workspaceId,
                                           final String environmentName,
                                           MachineInstanceCreator instanceCreator)
            throws NotFoundException,
                   SnapshotException,
                   ConflictException,
                   BadRequestException,
                   MachineException {
        final InstanceProvider instanceProvider = machineInstanceProviders.getProvider(machineConfig.getType());
        final String sourceType = machineConfig.getSource().getType();

        Recipe recipe = null;
        InstanceKey instanceKey = null;
        if ("Recipe".equalsIgnoreCase(sourceType)) {
            // TODO should we check that it is dockerfile?
            recipe = getRecipeByLocation(machineConfig);
        } else if ("Snapshot".equalsIgnoreCase(sourceType)) {
            instanceKey = snapshotDao.getSnapshot(machineConfig.getSource().getLocation()).getInstanceKey();
        } else {
            throw new BadRequestException("Source type is unsupported " + sourceType);
        }

        if (!MACHINE_DISPLAY_NAME_PATTERN.matcher(machineConfig.getName()).matches()) {
            throw new BadRequestException("Invalid machine name " + machineConfig.getName());
        }

        for (MachineStateImpl machine : machineRegistry.getStates()) {
            if (machine.getWorkspaceId().equals(workspaceId) && machine.getName().equals(machineConfig.getName())) {
                throw new ConflictException("Machine with name " + machineConfig.getName() + " already exists");
            }
        }

        final String machineId = generateMachineId();
        final String creator = EnvironmentContext.getCurrent().getUser().getId();

        if (machineConfig.getLimits().getMemory() == 0) {
            MachineConfigImpl machineConfigWithLimits = new MachineConfigImpl(machineConfig);
            machineConfigWithLimits.setLimits(new LimitsImpl(defaultMachineMemorySizeMB));
            machineConfig = machineConfigWithLimits;
        }

        final MachineStateImpl machineState = new MachineStateImpl(machineConfig.isDev(),
                                                                   machineConfig.getName(),
                                                                   machineConfig.getType(),
                                                                   machineConfig.getSource(),
                                                                   machineConfig.getLimits(),
                                                                   machineId,
                                                                   createMachineChannels(machineConfig.getName(),
                                                                                         workspaceId,
                                                                                         environmentName),
                                                                   workspaceId,
                                                                   creator,
                                                                   MachineStatus.CREATING);

        createMachineLogsDir(machineId);
        final LineConsumer machineLogger = getMachineLogger(machineId, machineState.getChannels().getOutput());

        try {
            machineRegistry.add(machineState);

            instanceCreator.createInstance(instanceProvider, recipe, instanceKey, machineState, machineLogger);

            return machineState;
        } catch (ConflictException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    private void createInstance(InstanceProvider instanceProvider,
                                Recipe recipe,
                                InstanceKey instanceKey,
                                MachineState machineState,
                                LineConsumer machineLogger) throws MachineException, NotFoundException {
        try {
            eventService.publish(DtoFactory.newDto(MachineStatusEvent.class)
                                           .withEventType(MachineStatusEvent.EventType.CREATING)
                                           .withMachineId(machineState.getId())
                                           .withDev(machineState.isDev())
                                           .withWorkspaceId(machineState.getWorkspaceId())
                                           .withMachineName(machineState.getName()));

            final Instance instance;
            if ("recipe".equalsIgnoreCase(machineState.getSource().getType())) {
                instance = instanceProvider.createInstance(recipe,
                                                           machineState,
                                                           machineLogger);
            } else {
                instance = instanceProvider.createInstance(instanceKey,
                                                           machineState,
                                                           machineLogger);
            }

            machineRegistry.update(instance);

            instance.setStatus(MachineStatus.RUNNING);

            eventService.publish(DtoFactory.newDto(MachineStatusEvent.class)
                                           .withEventType(MachineStatusEvent.EventType.RUNNING)
                                           .withDev(machineState.isDev())
                                           .withMachineId(machineState.getId())
                                           .withWorkspaceId(machineState.getWorkspaceId())
                                           .withMachineName(machineState.getName()));

        } catch (ServerException | ConflictException e) {
            eventService.publish(DtoFactory.newDto(MachineStatusEvent.class)
                                           .withEventType(MachineStatusEvent.EventType.ERROR)
                                           .withMachineId(machineState.getId())
                                           .withDev(machineState.isDev())
                                           .withWorkspaceId(machineState.getWorkspaceId())
                                           .withMachineName(machineState.getName())
                                           .withError(e.getLocalizedMessage()));

            try {
                machineLogger.writeLine(String.format("[ERROR] %s", e.getLocalizedMessage()));
                machineLogger.close();
                machineRegistry.remove(machineState.getId());
            } catch (IOException | NotFoundException e1) {
                LOG.error(e1.getLocalizedMessage());
            }
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    private interface MachineInstanceCreator {
        void createInstance(InstanceProvider instanceProvider, Recipe recipe, InstanceKey instanceKey, MachineState machineState,
                            LineConsumer machineLogger) throws MachineException, NotFoundException;
    }

    /**
     * Get machine by id
     *
     * @param machineId
     *         id of required machine
     * @return machine with specified id
     * @throws NotFoundException
     *         if machine with specified if not found
     */
    public Instance getMachine(String machineId) throws NotFoundException, MachineException {
        return machineRegistry.get(machineId);
    }

    /**
     * Get state of machine by id
     *
     * @param machineId
     *         id of required machine
     * @return machine state with specified id
     * @throws NotFoundException
     *         if machine with specified if not found
     */
    public MachineStateImpl getMachineState(String machineId) throws NotFoundException, MachineException {
        return machineRegistry.getState(machineId);
    }

    /**
     * Find machines connected with specific workspace/project
     *
     * @param owner
     *         id of owner of machine
     * @param workspaceId
     *         workspace binding
     * @return list of machines or empty list
     */
    public List<Instance> getMachines(String owner, String workspaceId) throws MachineException {
        return machineRegistry.getMachines()
                              .stream()
                              .filter(machine -> owner != null
                                                 && owner.equals(machine.getOwner())
                                                 && machine.getWorkspaceId().equals(workspaceId))
                              .collect(Collectors.toList());
    }

    public Instance getDevMachine(String workspaceId) throws NotFoundException, MachineException {
        return machineRegistry.getDevMachine(workspaceId);
    }

    /**
     * Find machines connected with specific workspace/project
     *
     * @param owner
     *         id of owner of machine
     * @param workspaceId
     *         workspace binding
     * @return list of machines or empty list
     */
    public List<MachineStateImpl> getMachinesStates(String owner, String workspaceId) throws MachineException {
        return machineRegistry.getStates()
                              .stream()
                              .filter(machine -> owner != null
                                                 && owner.equals(machine.getOwner())
                                                 && machine.getWorkspaceId().equals(workspaceId))
                              .collect(Collectors.toList());
    }

    /**
     * Saves state of machine to snapshot.
     *
     * @param machineId
     *         id of machine for saving
     * @param owner
     *         owner for new snapshot
     * @param description
     *         optional description that should help to understand purpose of new snapshot in future
     * @return {@link SnapshotImpl} that will be stored in background
     * @throws NotFoundException
     *         if machine with specified id doesn't exist
     * @throws MachineException
     *         if other error occur
     */
    public SnapshotImpl save(final String machineId, final String owner, final String description)
            throws NotFoundException, MachineException {
        final Instance machine = getMachine(machineId);

        final SnapshotImpl snapshot = new SnapshotImpl(generateSnapshotId(),
                                                       machine.getType(),
                                                       null,
                                                       owner,
                                                       System.currentTimeMillis(),
                                                       machine.getWorkspaceId(),
                                                       description,
                                                       machine.isDev());

        executor.submit(ThreadLocalPropagateContext.wrap(() -> {
            try {
                final InstanceKey instanceKey = machine.saveToSnapshot(machine.getOwner());
                snapshot.setInstanceKey(instanceKey);

                snapshotDao.saveSnapshot(snapshot);
            } catch (Exception e) {
                try {
                    machine.getLogger().writeLine("Snapshot storing failed. " + e.getLocalizedMessage());
                } catch (IOException ignore) {
                }
            }
        }));

        return snapshot;
    }

    /**
     * Get snapshot by id
     *
     * @param snapshotId
     *         id of required snapshot
     * @return snapshot with specified id
     * @throws NotFoundException
     *         if snapshot with provided id not found
     * @throws SnapshotException
     *         if other error occur
     */
    public SnapshotImpl getSnapshot(String snapshotId) throws NotFoundException, SnapshotException {
        return snapshotDao.getSnapshot(snapshotId);
    }

    /**
     * Gets list of Snapshots by project.
     *
     * @param owner
     *         id of owner of machine
     * @param workspaceId
     *         workspace binding
     * @return list of Snapshots
     * @throws SnapshotException
     *         if error occur
     */
    public List<SnapshotImpl> getSnapshots(String owner, String workspaceId) throws SnapshotException {
        return snapshotDao.findSnapshots(owner, workspaceId);
    }

    /**
     * Remove snapshot by id
     *
     * @param snapshotId
     *         id of snapshot to remove
     * @throws NotFoundException
     *         if snapshot with specified id not found
     * @throws SnapshotException
     *         if other error occurs
     */
    public void removeSnapshot(String snapshotId) throws NotFoundException, SnapshotException {
        final SnapshotImpl snapshot = getSnapshot(snapshotId);
        final String instanceType = snapshot.getType();
        final InstanceProvider instanceProvider = machineInstanceProviders.getProvider(instanceType);
        instanceProvider.removeInstanceSnapshot(snapshot.getInstanceKey());

        snapshotDao.removeSnapshot(snapshotId);
    }

    /**
     * Removes Snapshots by owner, workspace and project.
     *
     * @param owner
     *         owner of required snapshots
     * @param workspaceId
     *         workspace binding
     * @throws SnapshotException
     *         error occur
     */
    public void removeSnapshots(String owner, String workspaceId) throws SnapshotException {
        for (SnapshotImpl snapshot : snapshotDao.findSnapshots(owner, workspaceId)) {
            try {
                removeSnapshot(snapshot.getId());
            } catch (NotFoundException ignored) {
                // This is not expected since we just get list of snapshots from DAO.
            } catch (SnapshotException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Execute a command in machine
     *
     * @param machineId
     *         id of the machine where command should be executed
     * @param command
     *         command that should be executed in the machine
     * @return {@link org.eclipse.che.api.machine.server.spi.InstanceProcess} that represents started process in machine
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public InstanceProcess exec(final String machineId, final Command command, String outputChannel)
            throws NotFoundException, MachineException {
        final Instance machine = getMachine(machineId);
        final InstanceProcess instanceProcess = machine.createProcess(command.getCommandLine());
        final int pid = instanceProcess.getPid();

        final LineConsumer processLogger = getProcessLogger(machineId, pid, outputChannel);

        executor.execute(ThreadLocalPropagateContext.wrap(() -> {
            try {
                eventService.publish(newDto(MachineProcessEvent.class)
                                             .withEventType(MachineProcessEvent.EventType.STARTED)
                                             .withMachineId(machineId)
                                             .withProcessId(pid));

                instanceProcess.start(processLogger);

                eventService.publish(newDto(MachineProcessEvent.class)
                                             .withEventType(MachineProcessEvent.EventType.STOPPED)
                                             .withMachineId(machineId)
                                             .withProcessId(pid));
            } catch (ConflictException | MachineException error) {
                eventService.publish(newDto(MachineProcessEvent.class)
                                             .withEventType(MachineProcessEvent.EventType.ERROR)
                                             .withMachineId(machineId)
                                             .withProcessId(pid)
                                             .withError(error.getLocalizedMessage()));

                LOG.warn(error.getMessage());
                try {
                    processLogger.writeLine(String.format("[ERROR] %s", error.getMessage()));
                } catch (IOException ignored) {
                }
            }
        }));
        return instanceProcess;
    }

    /**
     * Get list of active processes from specific machine
     *
     * @param machineId
     *         id of machine to get processes information from
     * @return list of {@link org.eclipse.che.api.machine.server.spi.InstanceProcess}
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public List<InstanceProcess> getProcesses(String machineId) throws NotFoundException, MachineException {
        return getMachine(machineId).getProcesses();
    }

    /**
     * Stop process in machine
     *
     * @param machineId
     *         if of the machine where process should be stopped
     * @param pid
     *         id of the process that should be stopped in machine
     * @throws NotFoundException
     *         if machine or process with specified id not found
     * @throws ForbiddenException
     *         if process is finished already
     * @throws MachineException
     *         if other error occur
     */
    public void stopProcess(String machineId, int pid) throws NotFoundException, MachineException, ForbiddenException {
        final InstanceProcess process = getMachine(machineId).getProcess(pid);
        if (!process.isAlive()) {
            throw new ForbiddenException("Process finished already");
        }

        process.kill();

        eventService.publish(newDto(MachineProcessEvent.class)
                                     .withEventType(MachineProcessEvent.EventType.STOPPED)
                                     .withMachineId(machineId)
                                     .withProcessId(pid));
    }

    /**
     * Destroy machine with specified id
     *
     * @param machineId
     *         id of machine that should be destroyed
     * @param async
     *         should destroying be asynchronous or not
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public void destroy(final String machineId, boolean async) throws NotFoundException, MachineException {
        final Instance machine = getMachine(machineId);

        machine.setStatus(MachineStatus.DESTROYING);

        final MachineStateImpl machineState = machineRegistry.getState(machineId);

        eventService.publish(newDto(MachineStatusEvent.class)
                                     .withEventType(MachineStatusEvent.EventType.DESTROYING)
                                     .withMachineId(machineId)
                                     .withDev(machine.isDev())
                                     .withWorkspaceId(machineState.getWorkspaceId())
                                     .withMachineName(machineState.getName()));

        if (async) {
            executor.execute(ThreadLocalPropagateContext.wrap(() -> {
                try {
                    doDestroy(machine);
                } catch (NotFoundException | MachineException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }));
        } else {
            doDestroy(machine);
        }
    }

    /**
     * Gets logs reader from machine by specified id
     *
     * @param machineId
     *         machine id whose process reader will be returned
     * @return reader for logs on specified machine
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public Reader getMachineLogReader(String machineId) throws NotFoundException, MachineException {
        final File machineLogsFile = getMachineLogsFile(machineId);
        if (machineLogsFile.isFile()) {
            try {
                return Files.newBufferedReader(machineLogsFile.toPath(), Charset.defaultCharset());
            } catch (IOException e) {
                throw new MachineException(String.format("Unable read log file for machine '%s'. %s", machineId, e.getMessage()));
            }
        }
        throw new NotFoundException(String.format("Logs for machine '%s' are not available", machineId));
    }

    /**
     * Gets machines states.
     *
     * @return list of machine states
     * @throws MachineException
     *         if any error occur with machine registry
     */
    public List<MachineStateImpl> getMachinesStates() throws MachineException {
        return machineRegistry.getStates();
    }

    /**
     * Gets process reader from machine by specified id.
     *
     * @param machineId
     *         machine id whose process reader will be returned
     * @param pid
     *         process id
     * @return reader for specified process on machine
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public Reader getProcessLogReader(String machineId, int pid) throws NotFoundException, MachineException {
        final File processLogsFile = getProcessLogsFile(machineId, pid);
        if (processLogsFile.isFile()) {
            try {
                return Files.newBufferedReader(processLogsFile.toPath(), Charset.defaultCharset());
            } catch (IOException e) {
                throw new MachineException(
                        String.format("Unable read log file for process '%s' of machine '%s'. %s", pid, machineId, e.getMessage()));
            }
        }
        throw new NotFoundException(String.format("Logs for process '%s' of machine '%s' are not available", pid, machineId));
    }

    private void doDestroy(Instance machine) throws MachineException, NotFoundException {
        machine.destroy();

        cleanupOnDestroy(machine, null);
    }

    private void cleanupOnDestroy(Instance machine, String message) throws NotFoundException, MachineException {
        try {
            if (!Strings.isNullOrEmpty(message)) {
                machine.getLogger().writeLine(message);
            }
            machine.getLogger().close();
        } catch (IOException ignore) {
        }

        final MachineStateImpl machineState = machineRegistry.getState(machine.getId());

        machineRegistry.remove(machine.getId());

        eventService.publish(newDto(MachineStatusEvent.class)
                                     .withEventType(MachineStatusEvent.EventType.DESTROYED)
                                     .withDev(machine.isDev())
                                     .withMachineId(machine.getId())
                                     .withWorkspaceId(machineState.getWorkspaceId())
                                     .withMachineName(machineState.getName()));
    }

    private void createMachineLogsDir(String machineId) throws MachineException {
        if (!new File(machineLogsDir, machineId).mkdirs()) {
            throw new MachineException("Can't create folder for the logs of machine");
        }
    }

    private FileLineConsumer getMachineFileLogger(String machineId) throws MachineException {
        try {
            return new FileLineConsumer(getMachineLogsFile(machineId));
        } catch (IOException e) {
            throw new MachineException(String.format("Unable create log file for machine '%s'. %s", machineId, e.getMessage()));
        }
    }

    private File getMachineLogsFile(String machineId) {
        return new File(new File(machineLogsDir, machineId), "machineId.logs");
    }

    private File getProcessLogsFile(String machineId, int pid) {
        return new File(new File(machineLogsDir, machineId), Integer.toString(pid));
    }

    private FileLineConsumer getProcessFileLogger(String machineId, int pid) throws MachineException {
        try {
            return new FileLineConsumer(getProcessLogsFile(machineId, pid));
        } catch (IOException e) {
            throw new MachineException(
                    String.format("Unable create log file for process '%s' of machine '%s'. %s", pid, machineId, e.getMessage()));
        }
    }

    private String generateMachineId() {
        return NameGenerator.generate("machine", 16);
    }

    private String generateSnapshotId() {
        return NameGenerator.generate("snapshot", 16);
    }

    private LineConsumer getMachineLogger(String machineId, String outputChannel) throws MachineException {
        return getLogger(getMachineFileLogger(machineId), outputChannel);
    }

    private LineConsumer getProcessLogger(String machineId, int pid, String outputChannel) throws MachineException {
        return getLogger(getProcessFileLogger(machineId, pid), outputChannel);
    }

    private LineConsumer getLogger(LineConsumer fileLogger, String outputChannel) throws MachineException {
        if (outputChannel != null) {
            return new CompositeLineConsumer(fileLogger, new WebsocketLineConsumer(outputChannel));
        }
        return fileLogger;
    }

    public static ChannelsImpl createMachineChannels(String machineName, String workspaceId, String envName) {
        return new ChannelsImpl(workspaceId + ':' + envName + ':' + machineName,
                                "machine:status:" + workspaceId + ':' + machineName);
    }

    // cleanup machine if event about instance failure comes
    private class MachineCleaner implements EventSubscriber<InstanceStateEvent> {
        @Override
        public void onEvent(InstanceStateEvent event) {
            if ((event.getType() == OOM) || (event.getType() == DIE)) {
                try {
                    final Instance machine = getMachine(event.getMachineId());
                    String message = "Machine is destroyed. ";
                    if (event.getType() == OOM) {
                        message = message +
                                  "The processes in this machine need more RAM. This machine started with " +
                                  machine.getLimits().getMemory() +
                                  "MB. Create a new machine configuration that allocates additional RAM or increase " +
                                  "the workspace RAM limit in the user dashboard.";
                    }

                    cleanupOnDestroy(machine, message);
                } catch (NotFoundException | MachineException e) {
                    LOG.debug(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    Recipe getRecipeByLocation(MachineConfig machineConfig) throws MachineException {
        String recipeContent;
        File file = null;
        try {
            UriBuilder targetUriBuilder = UriBuilder.fromUri(machineConfig.getSource().getLocation());
            // add user token to be able to download user's private recipe
            if (machineConfig.getSource().getLocation().startsWith(apiEndpoint)) {
                if (EnvironmentContext.getCurrent().getUser() != null
                    && EnvironmentContext.getCurrent().getUser().getToken() != null) {
                    targetUriBuilder.queryParam("token", EnvironmentContext.getCurrent().getUser().getToken());
                }
            }
            file = IoUtil.downloadFile(null, "recipe", null, targetUriBuilder.build().toURL());
            recipeContent = IoUtil.readAndCloseQuietly(new FileInputStream(file));
        } catch (IOException | IllegalArgumentException e) {
            throw new MachineException("Can't start machine " + machineConfig.getName() + ". " + e.getLocalizedMessage());
        } finally {
            if (file != null) {
                FileCleaner.addFile(file);
            }
        }

        return new RecipeImpl().withType("Dockerfile").withScript(recipeContent);
    }

    private MachineImpl instanceToMachineImpl(Instance instance) {
        return MachineImpl.builder()
                          .setChannels(instance.getChannels())
                          .setLimits(instance.getLimits())
                          .setOwner(instance.getOwner())
                          .setStatus(instance.getStatus())
                          .setType(instance.getType())
                          .setSource(instance.getSource())
                          .setDev(instance.isDev())
                          .setId(instance.getId())
                          .setMetadata(instance.getMetadata())
                          .setName(instance.getName())
                          .setWorkspaceId(instance.getWorkspaceId())
                          .build();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void createLogsDir() {
        eventService.subscribe(machineCleaner);

        if (!(machineLogsDir.exists() || machineLogsDir.mkdirs())) {
            throw new IllegalStateException(String.format("Unable create directory %s", machineLogsDir.getAbsolutePath()));
        }
    }

    @PreDestroy
    private void cleanup() {
        eventService.unsubscribe(machineCleaner);

        boolean interrupted = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate main pool");
                }
            }
        } catch (InterruptedException e) {
            interrupted = true;
            executor.shutdownNow();
        }

        try {
            for (MachineStateImpl machine : machineRegistry.getStates()) {
                try {
                    destroy(machine.getId(), false);
                } catch (Exception e) {
                    LOG.warn(e.getMessage());
                }
            }
        } catch (MachineException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        final java.io.File[] files = machineLogsDir.listFiles();
        if (files != null && files.length > 0) {
            for (java.io.File f : files) {
                if (!IoUtil.deleteRecursive(f)) {
                    LOG.warn("Failed delete {}", f);
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
