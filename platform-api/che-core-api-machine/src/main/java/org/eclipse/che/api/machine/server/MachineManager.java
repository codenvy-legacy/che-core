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
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.FileLineConsumer;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.spi.Image;
import org.eclipse.che.api.machine.server.spi.ImageKey;
import org.eclipse.che.api.machine.server.spi.ImageProvider;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.Command;
import org.eclipse.che.api.machine.shared.MachineState;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Facade for Machine level operations.
 *
 * @author gazarenkov
 */
@Singleton
public class MachineManager {
    private static final Logger LOG = LoggerFactory.getLogger(MachineManager.class);

    private final SnapshotStorage            snapshotStorage;
    private final File                       machineLogsDir;
    private final Map<String, ImageProvider> imageProviders;
    private final ExecutorService            executor;
    private final MachineNode                machineNode;
    private final MachineRegistry            machineRegistry;

    @Inject
    public MachineManager(SnapshotStorage snapshotStorage,
                          Set<ImageProvider> imageProviders,
                          MachineRegistry machineRegistry,
                          MachineNode machineNode,
                          @Named("machine.logs.location") String machineLogsDir) {
        this.snapshotStorage = snapshotStorage;
        this.machineLogsDir = new File(machineLogsDir);
        this.machineNode = machineNode;
        this.imageProviders = new HashMap<>();
        this.machineRegistry = machineRegistry;
        for (ImageProvider provider : imageProviders) {
            this.imageProviders.put(provider.getType(), provider);
        }
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("MachineManager-%d").setDaemon(true).build());
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
                boolean deleted;
                if (f.isDirectory()) {
                    deleted = IoUtil.deleteRecursive(f);
                } else {
                    deleted = f.delete();
                }
                if (!deleted) {
                    LOG.warn("Failed delete {}", f);
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates and starts machine from scratch using recipe.
     *
     * @param machineType
     *         type of machine
     * @param recipe
     *         machine's recipe
     * @param owner
     *         owner for new machine
     * @param workspaceId
     *         workspace the machine is bound to
     * @param machineLogsOutput
     *         output for machine's logs
     * @return new Machine
     * @throws UnsupportedRecipeException
     *         if recipe isn't supported
     * @throws InvalidRecipeException
     *         if recipe is not valid
     * @throws MachineException
     *         if any other exception occurs during starting
     */
    public MachineImpl create(final String machineType,
                              final Recipe recipe,
                              final String workspaceId,
                              final String owner,
                              final LineConsumer machineLogsOutput)
            throws UnsupportedRecipeException, InvalidRecipeException, MachineException, NotFoundException {
        final ImageProvider imageProvider = imageProviders.get(machineType);
        if (imageProvider == null) {
            throw new NotFoundException(String.format("Unable create machine from recipe, unsupported machine type '%s'", machineType));
        }
        final String recipeType = recipe.getType();
        if (imageProvider.getRecipeTypes().contains(recipeType)) {
            final String machineId = generateMachineId();
            final CompositeLineConsumer machineLogger = new CompositeLineConsumer(machineLogsOutput, getMachineFileLogger(machineId));
            final MachineImpl machine = new MachineImpl(machineId, imageProvider.getType(), workspaceId, owner, machineLogger);
            machine.setState(MachineState.CREATING);
            machineRegistry.put(machine);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Image image = imageProvider.createImage(recipe, machineLogger);
                        final Instance instance = image.createInstance();
                        machine.setInstance(instance);
                        machine.setState(MachineState.RUNNING);
                    } catch (Exception error) {
                        try {
                            LOG.error(error.getMessage(), error);
                            machineRegistry.remove(machine.getId());
                            machineLogger.writeLine(String.format("[ERROR] %s", error.getMessage()));
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
     * @param snapshotId
     *         id of snapshot
     * @param owner
     *         owner for new machine
     * @param machineLogsOutput
     *         output for machine's creation logs
     * @return new Machine
     * @throws NotFoundException
     *         if snapshot not found
     * @throws InvalidImageException
     *         if Image pointed by snapshot is not valid
     * @throws MachineException
     *         if any other exception occurs during starting
     */
    public MachineImpl create(final String snapshotId, final String owner, final LineConsumer machineLogsOutput)
            throws NotFoundException, ServerException {
        final Snapshot snapshot = snapshotStorage.getSnapshot(snapshotId);
        final String imageType = snapshot.getImageType();
        final ImageProvider imageProvider = imageProviders.get(imageType);
        if (imageProvider == null) {
            throw new MachineException(
                    String.format("Unable create machine from snapshot '%s', unsupported image type '%s'", snapshotId, imageType));
        }
        final String machineId = generateMachineId();
        final CompositeLineConsumer machineLogger = new CompositeLineConsumer(machineLogsOutput, getMachineFileLogger(machineId));
        final MachineImpl machine = new MachineImpl(machineId, imageProvider.getType(), snapshot.getWorkspaceId(), owner, machineLogger);
        machine.setState(MachineState.CREATING);
        machineRegistry.put(machine);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Image image = imageProvider.createImage(snapshot.getImageKey(), machineLogger);
                    final Instance instance = image.createInstance();
                    machine.setInstance(instance);
                    machine.setState(MachineState.RUNNING);
                } catch (Exception error) {
                    try {
                        machineRegistry.remove(machine.getId());
                        LOG.error(error.getMessage());
                        machineLogger.writeLine(String.format("[ERROR] %s", error.getMessage()));
                        machineLogger.close();
                    } catch (IOException | NotFoundException e) {
                        LOG.error(e.getMessage());
                    }
                }
            }
        });
        return machine;
    }

    private String generateMachineId() {
        return NameGenerator.generate("machine", 16);
    }

    private FileLineConsumer getMachineFileLogger(String machineId) throws MachineException {
        try {
            return new FileLineConsumer(getMachineLogsFile(machineId));
        } catch (IOException e) {
            throw new MachineException(String.format("Unable create log file for machine '%s'. %s", machineId, e.getMessage()));
        }
    }

    private File getMachineLogsFile(String machineId) {
        return new File(machineLogsDir, machineId);
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

    public void bindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException, ForbiddenException {
        // TODO check that user has write permissions to bind project with synchronization
        final MachineImpl machine = getMachine(machineId);
        for (ProjectBinding projectBinding : machine.getProjects()) {
            if (projectBinding.getPath().equals(project.getPath())) {
                throw new ForbiddenException(String.format("Project %s is already bound to machine %s", project.getPath(), machineId));
            }
        }

        try {



            // TODO bindProject instead of machineNode
//            machine.bindProject(project);
            machineNode.copyProjectToMachine(machineId, project);

            machine.getProjects().add(project);

            machineNode.startSynchronization(machineId, project);
        } catch (Exception e) {
            try {
                machine.getMachineLogsOutput().writeLine("[ERROR] " + e.getLocalizedMessage());
            } catch (IOException ignored) {
            }
            throw new MachineException(e.getLocalizedMessage(), e);
        }
    }

    public void unbindProject(String machineId, ProjectBinding project) throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);

        // TODO unbindProject instead of machineNode
        machine.unbindProject(project);
        for (ProjectBinding projectBinding : machine.getProjects()) {
            if (projectBinding.getPath().equals(project.getPath())) {

                try {
                    machineNode.stopSynchronization(machineId, project);
                } catch (ServerException | NotFoundException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                    try {
                        machine.getMachineLogsOutput().writeLine("[ERROR] " + e.getLocalizedMessage());
                    } catch (IOException ignored) {
                    }
                }

                try {
                    machineNode.removeProjectFromMachine(machineId, project);
                } catch (ServerException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                    try {
                        machine.getMachineLogsOutput().writeLine("[ERROR] " + e.getLocalizedMessage());
                    } catch (IOException ignored) {
                    }
                }

                machine.getProjects().remove(project);
                return;
            }
        }
        throw new NotFoundException(String.format("Binding of project %s in machine %s not found", project.getPath(), machineId));
    }

    public List<ProjectBinding> getProjects(String machineId) throws NotFoundException, MachineException {
        return new ArrayList<>(getMachine(machineId).getProjects());
    }

    public MachineImpl getMachine(String machineId) throws NotFoundException {
        final MachineImpl machine = machineRegistry.get(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine '%s' does not exist", machineId));
        }
        return machine;
    }

    public List<MachineImpl> getMachines() throws ServerException {
        return machineRegistry.getAll();
    }

    /**
     * Machine(s) the project is bound to.
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
     * Saves machine to Snapshot storage.
     *
     * @param machineId
     *         id of machine for saving
     * @param owner
     *         owner for new snapshot
     * @param description
     *         optional description that should help to understand purpose of new snapshot in future
     * @return Future for Snapshot creation process
     */
    public Future<Snapshot> save(final String machineId, final String owner, final String description)
            throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);
        final Instance instance = machine.getInstance();
        if (instance == null) {
            throw new MachineException(
                    String.format("Unable save machine '%s' in image, machine isn't properly initialized yet", machineId));
        }
        return executor.submit(new Callable<Snapshot>() {
            @Override
            public Snapshot call() throws Exception {
                final ImageKey imageKey = instance.saveToImage(machine.getOwner());
                final Snapshot snapshot = new Snapshot(generateSnapshotId(),
                                                       machine.getType(),
                                                       imageKey,
                                                       owner,
                                                       System.currentTimeMillis(),
                                                       machine.getWorkspaceId(),
                                                       new ArrayList<>(machine.getProjects()),
                                                       description);
                snapshotStorage.saveSnapshot(snapshot);
                return snapshot;
            }
        });
    }

    private String generateSnapshotId() {
        return NameGenerator.generate("snapshot", 16);
    }

    public Snapshot getSnapshot(String snapshotId) throws NotFoundException, ServerException {
        return snapshotStorage.getSnapshot(snapshotId);
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
     */
    public List<Snapshot> getSnapshots(String owner, String workspaceId, ProjectBinding project) throws ServerException {
        return snapshotStorage.findSnapshots(owner, workspaceId, project);
    }

    public void removeSnapshot(String snapshotId) throws NotFoundException, ServerException {
        final Snapshot snapshot = getSnapshot(snapshotId);
        final String imageType = snapshot.getImageType();
        final ImageProvider imageProvider = imageProviders.get(imageType);
        if (imageProvider == null) {
            throw new MachineException(
                    String.format("Unable remove image from snapshot '%s', unsupported image type '%s'", snapshotId, imageType));
        }
        imageProvider.removeImage(snapshot.getImageKey());

        snapshotStorage.removeSnapshot(snapshotId);
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
     */
    public void removeSnapshots(String owner, String workspaceId, ProjectBinding project) throws ServerException {
        for (Snapshot snapshot : snapshotStorage.findSnapshots(owner, workspaceId, project)) {
            try {
                removeSnapshot(snapshot.getId());
            } catch (NotFoundException ignored) {
                // This is not expected since we just get list of snapshots from DAO.
            } catch (ServerException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    public ProcessImpl exec(final String machineId, final Command command, final LineConsumer commandOutput)
            throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);
        final Instance instance = machine.getInstance();
        if (instance == null) {
            throw new MachineException(
                    String.format("Unable execute command in machine '%s' in image, machine isn't properly initialized yet", machineId));
        }
        final InstanceProcess instanceProcess = instance.createProcess(command.getCommandLine());
        final Runnable execTask = new Runnable() {
            @Override
            public void run() {
                try {
                    instanceProcess.start(commandOutput);
                } catch (ConflictException | MachineException error) {
                    LOG.warn(error.getMessage());
                    try {
                        commandOutput.writeLine(String.format("[ERROR] %s", error.getMessage()));
                    } catch (IOException ignored) {
                    }
                }
            }
        };
        executor.execute(execTask);
        return new ProcessImpl(instanceProcess);
    }

    public List<ProcessImpl> getProcesses(String machineId) throws NotFoundException, MachineException {
        return getMachine(machineId).getProcesses();
    }

    public void stopProcess(String machineId, int processId) throws NotFoundException, MachineException, ForbiddenException {
        final ProcessImpl process = getMachine(machineId).getProcess(processId);
        if (!process.isAlive()) {
            throw new ForbiddenException("Process finished already");
        }

        process.kill();
    }

    public void destroy(final String machineId) throws NotFoundException, MachineException {
        final MachineImpl machine = getMachine(machineId);
        machine.setState(MachineState.DESTROYING);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    destroy(machine);
                    machineRegistry.remove(machine.getId());
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
        } catch (IOException e) {
            LOG.warn(e.getMessage());
        }
    }
}
