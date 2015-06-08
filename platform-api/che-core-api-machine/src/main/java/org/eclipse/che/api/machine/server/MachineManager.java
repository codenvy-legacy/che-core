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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.FileLineConsumer;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.WebsocketLineConsumer;
import org.eclipse.che.api.machine.server.dao.SnapshotDao;
import org.eclipse.che.api.machine.server.exception.InvalidInstanceSnapshotException;
import org.eclipse.che.api.machine.server.exception.InvalidRecipeException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.exception.UnsupportedRecipeException;
import org.eclipse.che.api.machine.server.impl.MachineImpl;
import org.eclipse.che.api.machine.server.impl.ProcessImpl;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.api.machine.shared.Command;
import org.eclipse.che.api.machine.shared.MachineState;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.dto.MachineFromRecipeMetadata;
import org.eclipse.che.api.machine.shared.dto.MachineFromSnapshotMetadata;
import org.eclipse.che.api.machine.shared.dto.event.MachineProcessEvent;
import org.eclipse.che.api.machine.shared.dto.event.MachineStateEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineManager {
    private static final Logger LOG = LoggerFactory.getLogger(MachineManager.class);

    private final SnapshotDao              snapshotDao;
    private final File                     machineLogsDir;
    private final MachineInstanceProviders machineInstanceProviders;
    private final ExecutorService          executor;
    private final MachineRegistry          machineRegistry;
    private final EventService             eventService;

    private final DtoFactory dtoFactory = DtoFactory.getInstance();

    @Inject
    public MachineManager(SnapshotDao snapshotDao,
                          MachineRegistry machineRegistry,
                          MachineInstanceProviders machineInstanceProviders,
                          @Named("machine.logs.location") String machineLogsDir,
                          EventService eventService) {
        this.snapshotDao = snapshotDao;
        this.machineInstanceProviders = machineInstanceProviders;
        this.eventService = eventService;
        this.machineLogsDir = new File(machineLogsDir);
        this.machineRegistry = machineRegistry;

        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("MachineManager-%d").setDaemon(true).build());
    }

    /**
     * Creates and starts machine from scratch using recipe.
     *
     * @param machineFromRecipeMetadata
     *         metadata that contains all information needed for creation machine from scratch
     * @return new Machine
     * @throws UnsupportedRecipeException
     *         if recipe isn't supported
     * @throws InvalidRecipeException
     *         if recipe is not valid
     * @throws NotFoundException
     *         if machine type from recipe is unsupported
     * @throws MachineException
     *         if any other exception occurs during starting
     */
    public MachineImpl create(final MachineFromRecipeMetadata machineFromRecipeMetadata)
            throws UnsupportedRecipeException, InvalidRecipeException, MachineException, NotFoundException {

        final InstanceProvider instanceProvider = machineInstanceProviders.getProvider(machineFromRecipeMetadata.getType());
        final String recipeType = machineFromRecipeMetadata.getRecipeDescriptor().getType();

        if (instanceProvider.getRecipeTypes().contains(recipeType)) {
            final String machineId = generateMachineId();

            createMachineLogsDir(machineId);
            final LineConsumer machineLogger = getMachineLogger(machineId, machineFromRecipeMetadata.getOutputChannel());

            final MachineImpl machine = new MachineImpl(machineId,
                                                        instanceProvider.getType(),
                                                        machineFromRecipeMetadata.getWorkspaceId(),
                                                        EnvironmentContext.getCurrent().getUser().getId(),
                                                        machineLogger,
                                                        machineFromRecipeMetadata.isBindWorkspace(),
                                                        machineFromRecipeMetadata.getDisplayName());
            machine.setState(MachineState.CREATING);
            machineRegistry.add(machine);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                       .withEventType(MachineStateEvent.EventType.CREATING)
                                                       .withMachineId(machineId));

                        final Instance instance = instanceProvider.createInstance(
                                RecipeImpl.fromDescriptor(machineFromRecipeMetadata.getRecipeDescriptor()),
                                machineLogger,
                                machineFromRecipeMetadata.getWorkspaceId(),
                                machineFromRecipeMetadata.isBindWorkspace());

                        machine.setInstance(instance);
                        machine.setState(MachineState.RUNNING);

                        eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                       .withEventType(MachineStateEvent.EventType.RUNNING)
                                                       .withMachineId(machineId));
                    } catch (Exception error) {
                        eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                       .withEventType(MachineStateEvent.EventType.ERROR)
                                                       .withMachineId(machineId)
                                                       .withError(error.getLocalizedMessage()));

                        try {
                            LOG.error(error.getLocalizedMessage(), error);
                            machineRegistry.remove(machine.getId());
                            machineLogger.writeLine(String.format("[ERROR] %s", error.getLocalizedMessage()));
                            machineLogger.close();
                        } catch (IOException | NotFoundException e) {
                            LOG.error(e.getMessage());
                        }
                    }
                }
            });
            return machine;
        } else {
            throw new UnsupportedRecipeException(String.format("Recipe of type '%s' is not supported", recipeType));
        }
    }

    /**
     * Restores and starts machine from snapshot.
     *
     * @param machineFromSnapshotMetadata
     *         metadata that contains all information needed for creation machine from snapshot
     * @return new machine
     * @throws NotFoundException
     *         if snapshot not found
     * @throws InvalidInstanceSnapshotException
     *         if instance pointed by snapshot is not valid
     * @throws SnapshotException
     *         if error occurs on retrieving snapshot information
     * @throws MachineException
     *         if any other exception occurs during starting
     */
    public MachineImpl create(MachineFromSnapshotMetadata machineFromSnapshotMetadata)
            throws NotFoundException, MachineException, InvalidInstanceSnapshotException, SnapshotException {
        final SnapshotImpl snapshot = getSnapshot(machineFromSnapshotMetadata.getSnapshotId());
        final String instanceType = snapshot.getType();

        final InstanceProvider instanceProvider = machineInstanceProviders.getProvider(instanceType);

        final String machineId = generateMachineId();
        createMachineLogsDir(machineId);

        final LineConsumer machineLogger = getMachineLogger(machineId, machineFromSnapshotMetadata.getOutputChannel());

        final MachineImpl machine = new MachineImpl(machineId,
                                                    instanceProvider.getType(),
                                                    snapshot.getWorkspaceId(),
                                                    EnvironmentContext.getCurrent().getUser().getId(),
                                                    machineLogger,
                                                    snapshot.isWorkspaceBound(),
                                                    machineFromSnapshotMetadata.getDisplayName());
        machine.setState(MachineState.CREATING);
        machineRegistry.add(machine);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                   .withEventType(MachineStateEvent.EventType.CREATING)
                                                   .withMachineId(machineId));

                    final Instance instance = instanceProvider.createInstance(snapshot.getInstanceKey(),
                                                                              machineLogger,
                                                                              snapshot.getWorkspaceId(),
                                                                              snapshot.isWorkspaceBound());
                    machine.setInstance(instance);
                    machine.setState(MachineState.RUNNING);

                    eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                   .withEventType(MachineStateEvent.EventType.RUNNING)
                                                   .withMachineId(machineId));
                } catch (Exception error) {
                    eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                   .withEventType(MachineStateEvent.EventType.ERROR)
                                                   .withMachineId(machineId)
                                                   .withError(error.getLocalizedMessage()));
                    try {
                        machineRegistry.remove(machine.getId());
                        LOG.error(error.getLocalizedMessage());
                        machineLogger.writeLine(String.format("[ERROR] %s", error.getLocalizedMessage()));
                        machineLogger.close();
                    } catch (IOException | NotFoundException e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
        });
        return machine;
    }

    /**
     * Get machine information by id
     *
     * @param machineId
     *         id of required machine
     * @return machine with specified id
     * @throws NotFoundException
     *         if machine with specified if not found
     */
    public MachineImpl getMachine(String machineId) throws NotFoundException {
        final MachineImpl machine = machineRegistry.get(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine '%s' does not exist", machineId));
        }
        return machine;
    }

    /**
     * Find machines connected with specific workspace/project
     *
     * @param owner
     *         id of owner of machine
     * @param workspaceId
     *         workspace binding
     * @param project
     *         project binding
     * @return list of machines or empty list
     */
    public List<MachineImpl> getMachines(String owner, String workspaceId, ProjectBinding project) {
        final List<MachineImpl> result = new LinkedList<>();
        for (MachineImpl machine : machineRegistry.getAll()) {
            if (owner != null && owner.equals(machine.getOwner()) &&
                machine.getWorkspaceId().equals(workspaceId)) {
                if (project != null) {
                    for (ProjectBinding projectBinding : machine.getProjects()) {
                        if (projectBinding.getPath().equals(project.getPath())) {
                            result.add(machine);
                        }
                    }
                } else {
                    result.add(machine);
                }
            }
        }
        return result;
    }

    /**
     * Bind project to machine
     *
     * @param machineId
     *         machine where project should be bound
     * @param project
     *         project that should be bound
     * @throws NotFoundException
     *         with specified id not found
     * @throws ForbiddenException
     *         if project is bound already to specified machine
     * @throws MachineException
     *         if other error occur
     */
    public void bindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException, ForbiddenException {
        // TODO check that user has write permissions to bind project with synchronization
        final MachineImpl machine = getMachine(machineId);
        for (ProjectBinding projectBinding : machine.getProjects()) {
            if (projectBinding.getPath().equals(project.getPath())) {
                throw new ForbiddenException(String.format("Project %s is already bound to machine %s", project.getPath(), machineId));
            }
        }

        try {
            machine.bindProject(project);
        } catch (Exception e) {
            try {
                machine.getMachineLogsOutput().writeLine("[ERROR] " + e.getLocalizedMessage());
            } catch (IOException ignored) {
            }
            LOG.error(e.getLocalizedMessage(), e);
            throw new MachineException(e.getLocalizedMessage());
        }
    }

    /**
     * Unbind project from machine
     *
     * @param machineId
     *         machine where project should be bound
     * @param project
     *         project that should be unbound
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public void unbindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);

        machine.unbindProject(project);
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
    public SnapshotImpl save(final String machineId, final String owner, final String label, final String description)
            throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);
        final Instance instance = machine.getInstance();
        if (instance == null) {
            throw new MachineException(
                    String.format("Unable save machine '%s' in instance, machine isn't properly initialized yet", machineId));
        }

        final SnapshotImpl snapshot = new SnapshotImpl(generateSnapshotId(),
                                                       machine.getType(),
                                                       null,
                                                       owner,
                                                       System.currentTimeMillis(),
                                                       machine.getWorkspaceId(),
                                                       new ArrayList<>(machine.getProjects()),
                                                       description,
                                                       label,
                                                       machine.isWorkspaceBound());

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final InstanceKey instanceKey = instance.saveToSnapshot(machine.getOwner(), label);
                    snapshot.setInstanceKey(instanceKey);

                    snapshotDao.saveSnapshot(snapshot);
                } catch (Exception e) {
                    try {
                        machine.getMachineLogsOutput().writeLine("Snapshot storing failed. " + e.getLocalizedMessage());
                    } catch (IOException ignore) {
                    }
                }
            }
        });

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
     * @param project
     *         project binding
     * @return list of Snapshots
     * @throws SnapshotException
     *         if error occur
     */
    public List<SnapshotImpl> getSnapshots(String owner, String workspaceId, ProjectBinding project) throws SnapshotException {
        return snapshotDao.findSnapshots(owner, workspaceId, project);
    }

    /**
     * Remove snapshot by id
     *
     * @param snapshotId
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
     * @param project
     *         project binding
     * @throws SnapshotException
     *         error occur
     */
    public void removeSnapshots(String owner, String workspaceId, ProjectBinding project) throws SnapshotException {
        for (SnapshotImpl snapshot : snapshotDao.findSnapshots(owner, workspaceId, project)) {
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
     * @param clientOutputChannel
     *         channel where client expects to see output of the command
     * @return {@link org.eclipse.che.api.machine.server.impl.ProcessImpl} that represents started process in machine
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public ProcessImpl exec(final String machineId, final Command command, String clientOutputChannel)
            throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);
        final Instance instance = machine.getInstance();
        if (instance == null) {
            throw new MachineException(
                    String.format("Unable execute command in machine '%s' in instance, machine isn't properly initialized yet", machineId));
        }
        final InstanceProcess instanceProcess = instance.createProcess(command.getCommandLine());
        final int pid = instanceProcess.getPid();
        final LineConsumer processLogger = getProcessLogger(machineId, pid, clientOutputChannel);
        final Runnable execTask = new Runnable() {
            @Override
            public void run() {
                try {
                    eventService.publish(dtoFactory.createDto(MachineProcessEvent.class)
                                                   .withEventType(MachineProcessEvent.EventType.STARTED)
                                                   .withMachineId(machineId)
                                                   .withProcessId(pid));

                    instanceProcess.start(processLogger);

                    eventService.publish(dtoFactory.createDto(MachineProcessEvent.class)
                                                   .withEventType(MachineProcessEvent.EventType.STOPPED)
                                                   .withMachineId(machineId)
                                                   .withProcessId(pid));
                } catch (ConflictException | MachineException error) {
                    eventService.publish(dtoFactory.createDto(MachineProcessEvent.class)
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
            }
        };
        executor.execute(execTask);
        return new ProcessImpl(instanceProcess);
    }

    /**
     * Get list of active processes from specific machine
     *
     * @param machineId
     *         id of machine to get processes information from
     * @return list of {@link org.eclipse.che.api.machine.server.impl.ProcessImpl}
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public List<ProcessImpl> getProcesses(String machineId) throws NotFoundException, MachineException {
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
        final ProcessImpl process = getMachine(machineId).getProcess(pid);
        if (!process.isAlive()) {
            throw new ForbiddenException("Process finished already");
        }

        process.kill();

        eventService.publish(dtoFactory.createDto(MachineProcessEvent.class)
                                       .withEventType(MachineProcessEvent.EventType.STOPPED)
                                       .withMachineId(machineId)
                                       .withProcessId(pid));
    }

    /**
     * Destroy machine with specified id
     *
     * @param machineId
     *         id of machine that should be destroyed
     * @throws NotFoundException
     *         if machine with specified id not found
     * @throws MachineException
     *         if other error occur
     */
    public void destroy(final String machineId) throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);
        machine.setState(MachineState.DESTROYING);
        eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                       .withEventType(MachineStateEvent.EventType.DESTROYING)
                                       .withMachineId(machineId));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    destroy(machine);
                    machineRegistry.remove(machine.getId());

                    eventService.publish(dtoFactory.createDto(MachineStateEvent.class)
                                                   .withEventType(MachineStateEvent.EventType.DESTROYED)
                                                   .withMachineId(machineId));
                } catch (MachineException | NotFoundException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
    }

    private void destroy(MachineImpl machine) throws MachineException {
        final Instance instance = machine.getInstance();
        if (instance != null) {
            instance.destroy();
            machine.setInstance(null);
        }
        try {
            machine.getMachineLogsOutput().close();
        } catch (IOException ignore) {
        }
    }

    public List<MachineImpl> getMachines() {
        return machineRegistry.getAll();
    }

    public List<ProjectBinding> getProjects(String machineId) throws NotFoundException, MachineException {
        return new ArrayList<>(getMachine(machineId).getProjects());
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

    @PostConstruct
    private void createLogsDir() {
        if (!(machineLogsDir.exists() || machineLogsDir.mkdirs())) {
            throw new IllegalStateException(String.format("Unable create directory %s", machineLogsDir.getAbsolutePath()));
        }
    }

    @PreDestroy
    private void cleanup() {
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

        for (MachineImpl machine : machineRegistry.getAll()) {
            try {
                destroy(machine);
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
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
